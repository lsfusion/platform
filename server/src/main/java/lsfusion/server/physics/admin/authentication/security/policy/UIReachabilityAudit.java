package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.open.FormAction;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.navigator.NavigatorAction;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 0 (audit only, never denies) of the "synchronize what the user can see with what they can request" idea.
 *
 * The check compares dynamically executed / API accessed properties against the data the user can already reach
 * through the UI, normalized to STORED properties on both sides (the "hull"). Normalization is what makes the
 * comparison meaningful: forms draw composite properties (name(department(e))), scripts read the raw ones
 * (name(Department)), so comparing property objects directly would produce false alarms in both directions.
 *
 * V(role) = union of hulls of the properties drawn on the forms UI-reachable for that role, where reachability is
 * the transitive closure: navigator-permitted forms -> forms opened by the actions drawn on them (SHOW / DIALOG /
 * PRINT / EXPORT, including the auto-generated class forms behind the default edit / dialog actions) -> and so on.
 *
 * Known and accepted imprecision (all of it in the "would allow" direction, so the audit under-reports rather than
 * cries wolf): form filters are not representable here (seeing "salaries of my department" on a form marks the
 * whole salary data as reachable), and SHOWIF / tabs / design are ignored - a property drawn but never actually
 * displayed still counts as reachable.
 */
public class UIReachabilityAudit {

    // keyed by the role policy list, not by the SecurityPolicy object : a SecurityPolicy is built per connection,
    // while RoleSecurityPolicy instances are cached per role by SecurityManager - so the key is stable across
    // connections of the same role set, and changing the policy (which drops those caches) invalidates it for free
    private static final Map<List<RoleSecurityPolicy>, ImSet<Property>> cachedReachableData = new HashMap<>();

    public static void auditRead(BusinessLogics BL, SecurityPolicy policy, ActionOrProperty property, String source) {
        if (!Settings.get().isEnableUIReachabilityAudit() || policy == null)
            return;

        if (property instanceof Action) // actions are audited by reachability of the action itself, not of its data
            return;

        ImSet<Property> hull = getStoredHull((Property) property);
        if (hull.isEmpty()) // no stored data behind it (formulas, currentDateTime, ...) - nothing to compare
            return;

        ImSet<Property> reachable = getReachableData(BL, policy);
        ImSet<Property> outside = hull.filterFn(stored -> !reachable.contains(stored));
        if (!outside.isEmpty())
            ServerLoggers.systemLogger.warn(String.format(
                    "UI reachability audit (%s): user %s accessed %s, stored data outside of what the UI exposes: %s",
                    source, currentUserName(), property.getCanonicalName(), names(outside)));
    }

    public static void auditAction(BusinessLogics BL, SecurityPolicy policy, Action<?> action, String source) {
        if (!Settings.get().isEnableUIReachabilityAudit() || policy == null)
            return;

        // endpoints the developer explicitly published for programmatic calls (and the ones the client itself calls,
        // like Authentication.getCurrentUserLocale after login) are not UI elements - being "drawn on a form" is not
        // a meaningful expectation for them
        if (action.hasAnnotation("api") || action.hasAnnotation("noauth"))
            return;

        if (!getReachableActions(BL, policy).contains(action))
            ServerLoggers.systemLogger.warn(String.format(
                    "UI reachability audit (%s): user %s executed %s which is not drawn on any UI-reachable form",
                    source, currentUserName(), action.getCanonicalName()));
    }

    private static String currentUserName() {
        LogInfo logInfo = ThreadLocalContext.getLogInfo(Thread.currentThread());
        return logInfo != null && logInfo.userName != null ? logInfo.userName : "<unknown>";
    }

    private static String names(ImSet<Property> properties) {
        return properties.toJavaSet().stream().map(Property::getCanonicalName).collect(Collectors.joining(", "));
    }

    // getRecDepends is the transitive dependency set and is already cached on the property itself
    private static ImSet<Property> getStoredHull(Property<?> property) {
        MSet<Property> mResult = SetFact.mSet();
        if (property.isStored())
            mResult.add(property);
        for (Property depend : property.getRecDepends())
            if (depend.isStored())
                mResult.add(depend);
        return mResult.immutable();
    }

    private static synchronized ImSet<Property> getReachableData(BusinessLogics BL, SecurityPolicy policy) {
        ImSet<Property> result = cachedReachableData.get(policy.policies);
        if (result == null) {
            MSet<Property> mResult = SetFact.mSet();
            for (ActionOrProperty drawn : getReachableDraws(BL, policy))
                if (drawn instanceof Property)
                    mResult.addAll(getStoredHull((Property) drawn));
            result = mResult.immutable();
            cachedReachableData.put(policy.policies, result);
        }
        return result;
    }

    private static Set<Action<?>> getReachableActions(BusinessLogics BL, SecurityPolicy policy) {
        Set<Action<?>> result = new HashSet<>();
        for (ActionOrProperty drawn : getReachableDraws(BL, policy))
            if (drawn instanceof Action)
                result.add((Action<?>) drawn);
        return result;
    }

    // everything drawn on the UI-reachable forms (see the transitive closure in the class javadoc)
    private static Set<ActionOrProperty> getReachableDraws(BusinessLogics BL, SecurityPolicy policy) {
        Set<ActionOrProperty> result = new HashSet<>();
        Set<FormEntity> visited = new HashSet<>();

        Set<FormEntity> current = new HashSet<>();
        for (NavigatorElement element : BL.getNavigatorElements())
            if (element instanceof NavigatorAction && policy.checkNavigatorPermission(element)) {
                FormEntity form = ((NavigatorAction) element).getForm();
                if (form != null)
                    current.add(form);
            }

        while (!current.isEmpty()) {
            Set<FormEntity> next = new HashSet<>();
            for (FormEntity form : current) {
                if (!visited.add(form))
                    continue;

                for (PropertyDrawEntity<?, ?> draw : form.getPropertyDrawsListIt()) {
                    ActionOrProperty drawn = draw.getSecurityProperty();
                    if (!policy.checkPropertyViewPermission(drawn))
                        continue; // not visible to this role - doesn't expose anything
                    result.add(drawn);

                    // an action the user can execute opens more forms, so their data is reachable too. Note that the
                    // navigator permission is deliberately NOT consulted for these: a form hidden from the menu but
                    // opened by an allowed button is genuinely reachable (a standard pattern)
                    if (drawn instanceof Action && policy.checkDirectActionAccess((Action) drawn))
                        addOpenedForms(BL, (Action<?>) drawn, next, new HashSet<>());
                }
            }
            current = next;
        }
        return result;
    }

    private static void addOpenedForms(BusinessLogics BL, Action<?> action, Set<FormEntity> forms, Set<Action> visited) {
        if (!visited.add(action))
            return;

        if (action instanceof FormAction) {
            // getStaticForm also resolves the auto-generated class forms (ClassFormSelector behind the default
            // edit / dialog actions), which is what makes "form -> button -> auto form" chains work
            FormEntity form = ((FormAction<?>) action).form.getStaticForm(BL);
            if (form != null)
                forms.add(form);
        }

        for (Action<?> depend : action.getDependActions())
            addOpenedForms(BL, depend, forms, visited);
    }
}

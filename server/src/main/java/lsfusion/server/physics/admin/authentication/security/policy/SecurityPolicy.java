package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.base.BaseUtils;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class SecurityPolicy {

    List<RoleSecurityPolicy> policies;

    public SecurityPolicy() {
        this(new ArrayList<>());
    }

    public SecurityPolicy(List<RoleSecurityPolicy> policies) {
        this.policies = policies;
    }

    public SecurityPolicy add(RoleSecurityPolicy policy) {
        return new SecurityPolicy(BaseUtils.add(policies, policy));
    }

    public boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        // a permission is given to an element of the logics, and a client whose navigator is customized draws and runs copies of them
        NavigatorElement permissionElement = navigatorElement.getCustomizeElement();
        return checkPermission(policy -> policy.checkNavigatorPermission(permissionElement));
    }

    /** True iff the user can run a script in the {@code System.interpreter} form (from {@code utils/Eval.lsf}): has the
     *  UI ({@code enableUI}, the user's role's), may open the form from the navigator, and may see and run its
     *  {@code executeScript}, the run the web client draws. Used to derive HTTP API access: the API gives such a user
     *  nothing the interpreter does not, while a user who may open the form but not run it gets no API by it. */
    public boolean canDeriveAPIAccess(BusinessLogics bl) {
        NavigatorElement form = bl.findNavigatorElement("System.interpreter");
        LA<?> run = bl.findAction("System.executeScript[System.Script]");
        return Settings.get().getEnableUI() > 0 && form != null && run != null && checkNavigatorPermission(form)
                && checkPropertyViewPermission(run.action) && checkPropertyChangePermission(run.action, run.action);
    }

    public boolean checkPropertyViewPermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyViewPermission(property));
    }

    public boolean checkPropertyChangePermission(ActionOrProperty property, Action eventAction) {
        return checkPermission(policy -> policy.checkPropertyChangePermission(property, eventAction));
    }

    public boolean checkPropertyEditObjectsPermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyEditObjectsPermission(property));
    }

    public boolean checkPropertyGroupChangePermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyGroupChangePermission(property));
    }

    public Boolean checkPermission(Function<RoleSecurityPolicy, Boolean> checkPermission) {
        boolean result = true;
        for(RoleSecurityPolicy policy : policies) {
            Boolean permission = checkPermission.apply(policy);
            if(permission != null) {
                if(permission)
                    return true;
                else
                    result = false;
            }
        }
        return result;
    }
}

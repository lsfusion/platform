package lsfusion.server.logics.form.interactive.action.edit;

// pairs the form session scope of a property CHANGE with the APPLY flag (commit-on-edit):
// the scope drives the dialog/input/write session; APPLY additionally commits the change at the end of that session.
// it's a cache key for the @IdentityStrongLazy getDefaultEventAction, so it must define equals/hashCode (CacheAspect keys args by equals).
public class ChangeEventScope {
    public final FormSessionScope scope; // null - use the contextual default scope
    public final boolean apply;

    public ChangeEventScope(FormSessionScope scope, boolean apply) {
        this.scope = scope;
        this.apply = apply;
    }

    public static FormSessionScope getScope(ChangeEventScope eventScope) {
        return eventScope == null ? null : eventScope.scope;
    }
    public static FormSessionScope getScope(ChangeEventScope eventScope, FormSessionScope defaultScope) {
        FormSessionScope scope = getScope(eventScope);
        return scope == null ? defaultScope : scope;
    }
    public static boolean isApply(ChangeEventScope eventScope) {
        return eventScope != null && eventScope.apply;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof ChangeEventScope)) return false;
        ChangeEventScope that = (ChangeEventScope) o;
        return apply == that.apply && scope == that.scope;
    }
    @Override
    public int hashCode() {
        return 31 * (scope == null ? 0 : scope.hashCode()) + (apply ? 1 : 0);
    }
}

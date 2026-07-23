package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

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
        return checkPermission(policy -> policy.checkNavigatorPermission(navigatorElement));
    }

    /** True iff this policy lets the user open the {@code System.interpreter} form (from
     *  {@code utils/Eval.lsf}). The form itself is the meaningful privilege — once open it
     *  hosts every script / action / form / java-body execution wrapper. Used to derive
     *  HTTP API access: if the user can execute arbitrary lsf via the browser, blocking
     *  HTTP API on the same account is security theatre. */
    public boolean canDeriveAPIAccess(BusinessLogics bl) {
        NavigatorElement form = bl.findNavigatorElement("System.interpreter");
        return form != null && checkNavigatorPermission(form);
    }

    public boolean checkPropertyViewPermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyViewPermission(property));
    }

    public boolean checkPropertyChangePermission(ActionOrProperty property, Action eventAction) {
        return checkPermission(policy -> policy.checkPropertyChangePermission(property, eventAction));
    }

    // direct (non-form) property change, e.g. in dynamically executed code
    public boolean checkPropertyChangePermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyChangePermission(property));
    }

    // direct (non-form) access to a named element - dynamically executed code (EVAL) and API calls (/exec, JS controller) :
    // executing an action requires the same permissions as on a form - to see it and to change (execute) it
    public boolean checkDirectActionAccess(Action action) {
        return checkPropertyViewPermission(action) && checkPropertyChangePermission(action, action);
    }
    public boolean checkDirectPropertyChangeAccess(ActionOrProperty property) {
        return checkPropertyViewPermission(property) && checkPropertyChangePermission(property);
    }

    public boolean hasForbidden() {
        for(RoleSecurityPolicy policy : policies)
            if(policy.hasForbidden())
                return true;
        return false;
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

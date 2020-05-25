package lsfusion.server.physics.admin.authentication.security.policy;

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

    public boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        return checkPermission(policy -> policy.checkNavigatorPermission(navigatorElement));
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

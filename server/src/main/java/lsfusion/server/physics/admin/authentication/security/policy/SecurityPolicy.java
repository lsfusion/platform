package lsfusion.server.physics.admin.authentication.security.policy;

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

    public Boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        return checkPermission(policy -> policy.checkNavigatorPermission(navigatorElement));
    }

    public Boolean checkPropertyViewPermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyViewPermission(property));
    }

    public Boolean checkPropertyChangePermission(ActionOrProperty property) {
        return checkPermission(policy -> policy.checkPropertyChangePermission(property));
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

    public boolean checkForbidEditObjects() {
        for(RoleSecurityPolicy policy : policies) {
            if(!policy.checkForbidEditObjects()) {
                return false;
            }
        }
        return true;
    }
}

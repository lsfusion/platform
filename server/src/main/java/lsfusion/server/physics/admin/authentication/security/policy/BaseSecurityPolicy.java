package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

import java.util.ArrayList;
import java.util.List;

public class BaseSecurityPolicy implements SecurityPolicy {

    List<AddSecurityPolicy> policies;

    public BaseSecurityPolicy() {
        this(new ArrayList<>());
    }

    public BaseSecurityPolicy(List<AddSecurityPolicy> policies) {
        this.policies = policies;
    }

    @Override
    public Boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        boolean result = true;
        for(AddSecurityPolicy policy : policies) {
            Boolean permission = policy.checkNavigatorPermission(navigatorElement);
            if(permission != null) {
                if(permission)
                    return true;
                else
                    result = false;
            }
        }
        return result;
    }

    @Override
    public Boolean checkPropertyViewPermission(ActionOrProperty property) {
        boolean result = true;
        for(AddSecurityPolicy policy : policies) {
            Boolean permission = policy.checkPropertyViewPermission(property);
            if(permission != null) {
                if(permission)
                    return true;
                else
                    result = false;
            }
        }
        return result;
    }

    @Override
    public Boolean checkPropertyChangePermission(ActionOrProperty property) {
        boolean result = true;
        for(AddSecurityPolicy policy : policies) {
            Boolean permission = policy.checkPropertyChangePermission(property);
            if(permission != null) {
                if(permission)
                    return true;
                else
                    result = false;
            }
        }
        return result;
    }

    @Override
    public boolean checkForbidEditObjects() {
        for(AddSecurityPolicy policy : policies) {
            if(!policy.checkForbidEditObjects()) {
                return false;
            }
        }
        return true;
    }
}

package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class SecurityPolicy {
    public final long ID;
    public Boolean editObjects;

    public SecurityPolicy() {
        this(-1);
    }

    public SecurityPolicy(long ID) {
        this.ID = ID;
    }

    public PropertySecurityPolicy property = new PropertySecurityPolicy();
    public NavigatorSecurityPolicy navigator = new NavigatorSecurityPolicy();

    public void override(SecurityPolicy policy) {
        property.override(policy.property);
        navigator.override(policy.navigator);

        if (policy.editObjects != null) {
            editObjects = policy.editObjects;
        }
    }

    public static boolean checkPropertyViewPermission(ImSet<SecurityPolicy> securityPolicies, ActionOrProperty property) {
        for(SecurityPolicy securityPolicy : securityPolicies)
            if(!securityPolicy.property.view.checkPermission(property))
                return false;
        return true;
    }

    public static boolean checkPropertyChangePermission(ImSet<SecurityPolicy> securityPolicies, ActionOrProperty property) {
        for(SecurityPolicy securityPolicy : securityPolicies)
            if(!securityPolicy.property.change.checkPermission(property))
                return false;
        return true;
    }

}

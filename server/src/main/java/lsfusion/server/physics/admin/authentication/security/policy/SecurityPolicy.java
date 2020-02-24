package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
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

    public ClassSecurityPolicy cls = new ClassSecurityPolicy();
    public PropertySecurityPolicy property = new PropertySecurityPolicy();
    public NavigatorSecurityPolicy navigator = new NavigatorSecurityPolicy();
    public FormSecurityPolicy form = new FormSecurityPolicy();

    public void override(SecurityPolicy policy) {
        cls.override(policy.cls);
        property.override(policy.property);
        navigator.override(policy.navigator);
        form.override(policy.form);

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

    public static boolean checkClassChangePermission(ImSet<SecurityPolicy> securityPolicies, ConcreteCustomClass customClass) {
        for(SecurityPolicy securityPolicy : securityPolicies)
            if (!securityPolicy.cls.edit.change.checkPermission(customClass))
                return false;
        return true;
    }
}

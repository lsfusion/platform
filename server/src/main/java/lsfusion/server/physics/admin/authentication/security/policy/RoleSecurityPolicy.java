package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class RoleSecurityPolicy {
    public ElementSecurityPolicy navigator = new ElementSecurityPolicy<NavigatorElement>();
    public ElementSecurityPolicy propertyView = new ElementSecurityPolicy<ActionOrProperty>();
    public ElementSecurityPolicy propertyChange = new ElementSecurityPolicy<ActionOrProperty>();
    public ElementSecurityPolicy propertyEditObjects = new ElementSecurityPolicy<ActionOrProperty>();

    public RoleSecurityPolicy() {

    }

    public Boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        return this.navigator.checkPermission(navigatorElement);
    }

    public Boolean checkPropertyViewPermission(ActionOrProperty property) {
        return this.propertyView.checkPermission(property);
    }

    public Boolean checkPropertyChangePermission(ActionOrProperty property) {
        return this.propertyChange.checkPermission(property);
    }

    public Boolean checkPropertyEditObjectsPermission(ActionOrProperty property) {
        return this.propertyEditObjects.checkPermission(property);
    }
}

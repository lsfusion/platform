package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class AddSecurityPolicy implements SecurityPolicy {
    public AbstractSecurityPolicy navigator = new AbstractSecurityPolicy<NavigatorElement>();
    public AbstractSecurityPolicy propertyView = new AbstractSecurityPolicy<ActionOrProperty>();
    public AbstractSecurityPolicy propertyChange = new AbstractSecurityPolicy<ActionOrProperty>();

    public boolean forbidEditObjects;

    public AddSecurityPolicy() {

    }

    @Override
    public Boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        return this.navigator.checkPermission(navigatorElement);
    }

    @Override
    public Boolean checkPropertyViewPermission(ActionOrProperty property) {
        return this.propertyView.checkPermission(property);
    }

    @Override
    public Boolean checkPropertyChangePermission(ActionOrProperty property) {
        return this.propertyChange.checkPermission(property);
    }

    @Override
    public boolean checkForbidEditObjects() {
        return forbidEditObjects;
    }
}

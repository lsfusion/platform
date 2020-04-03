package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public interface SecurityPolicy {

    Boolean checkNavigatorPermission(NavigatorElement navigatorElement);

    Boolean checkPropertyViewPermission(ActionOrProperty property);

    Boolean checkPropertyChangePermission(ActionOrProperty property);

    boolean checkForbidEditObjects();

}

package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;

public class RoleSecurityPolicy {
    public ElementSecurityPolicy navigator = new ElementSecurityPolicy<NavigatorElement>();
    public ElementSecurityPolicy propertyView = new ElementSecurityPolicy<ActionOrProperty>();
    public ElementSecurityPolicy propertyChange = new ElementSecurityPolicy<ActionOrProperty>();
    public ElementSecurityPolicy propertyEditObjects = new ElementSecurityPolicy<ActionOrProperty>();

    public final boolean isReadOnlyPolicy;

    public RoleSecurityPolicy(boolean isReadOnlyPolicy) {
        this.isReadOnlyPolicy = isReadOnlyPolicy;
    }

    public Boolean checkNavigatorPermission(NavigatorElement navigatorElement) {
        return this.navigator.checkPermission(navigatorElement);
    }

    public Boolean checkPropertyViewPermission(ActionOrProperty property) {
        if((isReadOnlyPolicy || !Settings.get().isDisableActionForbidViewOnForbidChange()) && property instanceof Action) { // if action is cannot be changed don't show it in read only policy (maybe later do it for all policies)
            Boolean changePermission = checkPropertyChangePermission(property, (Action) property);
            if(changePermission != null && !changePermission) // change is forbidden - forbid view
                return false;
        }
        return this.propertyView.checkPermission(property);
    }

    public Boolean checkPropertyChangePermission(ActionOrProperty property, Action changeAction) {
        if((isReadOnlyPolicy || !Settings.get().isDisableDefaultChangeOnReadOnlyChange()) && changeAction.ignoreReadOnlyPolicy()) // if event handler doesn't change anything (for example SELECTOR), consider this event to be binding (not edit)
            return null;
        return this.propertyChange.checkPermission(property);
    }

    public Boolean checkPropertyEditObjectsPermission(ActionOrProperty property) {
        return this.propertyEditObjects.checkPermission(property);
    }
}

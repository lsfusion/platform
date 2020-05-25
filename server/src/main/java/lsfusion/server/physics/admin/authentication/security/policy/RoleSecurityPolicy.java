package lsfusion.server.physics.admin.authentication.security.policy;

import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

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
        if(isReadOnlyPolicy && property instanceof Action && !checkPropertyChangePermission(property, (Action) property)) // if action is cannot be changed don't show it in read only policy (maybe later do it for all policies)
            return false;
        return this.propertyView.checkPermission(property);
    }

    public Boolean checkPropertyChangePermission(ActionOrProperty property, Action eventAction) {
        if(isReadOnlyPolicy && eventAction.ignoreReadOnlyPolicy()) // if event handler doesn't change anything (for example SELECTOR), consider this event to be binding (not edit)
            return true;
        return this.propertyChange.checkPermission(property);
    }

    public Boolean checkPropertyEditObjectsPermission(ActionOrProperty property) {
        return this.propertyEditObjects.checkPermission(property);
    }
}

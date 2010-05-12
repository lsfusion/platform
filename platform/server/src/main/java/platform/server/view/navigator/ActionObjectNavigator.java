package platform.server.view.navigator;

import platform.server.logics.action.ActionInterface;
import platform.server.logics.action.Action;
import platform.server.logics.linear.LA;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.ActionObjectImplement;

import java.util.Map;

public class ActionObjectNavigator<P extends ActionInterface> extends ControlObjectNavigator<P, Action<P>, ActionObjectImplement<P>> {

    public ActionObjectNavigator(LA<P> property, ControlInterfaceNavigator... objects) {
        super(property, objects);
    }

    public ActionObjectNavigator(Action<P> property, Map<P, ControlInterfaceNavigator> mapping) {
        super(property, mapping);
    }

    public ControlViewNavigator createView(int ID, GroupObjectNavigator groupObject) {
        return new ActionViewNavigator<P>(ID, this, groupObject);
    }

    public ActionObjectImplement<P> createImplement(Map<P, PropertyObjectInterface> mapping) {
        return new ActionObjectImplement<P>(property, mapping);
    }
}

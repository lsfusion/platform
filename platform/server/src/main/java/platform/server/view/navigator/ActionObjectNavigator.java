package platform.server.view.navigator;

import platform.server.logics.action.ActionInterface;
import platform.server.logics.action.Action;
import platform.server.logics.linear.LA;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.ActionObjectImplement;

import java.util.Map;

public class ActionObjectNavigator extends ControlObjectNavigator<ActionInterface, Action, ActionObjectImplement> {

    public ActionObjectNavigator(LA property, ControlInterfaceNavigator... objects) {
        super(property, objects);
    }

    public ActionObjectNavigator(Action property, Map<ActionInterface, ControlInterfaceNavigator> mapping) {
        super(property, mapping);
    }

    public ControlViewNavigator createView(int ID, GroupObjectNavigator groupObject) {
        return new ActionViewNavigator(ID, this, groupObject);
    }

    public ActionObjectImplement createImplement(Map<ActionInterface, PropertyObjectInterface> mapping) {
        return new ActionObjectImplement(property, mapping);
    }
}

package platform.server.view.navigator;

import platform.server.logics.control.ControlInterface;
import platform.server.logics.action.Action;
import platform.server.logics.action.ActionInterface;
import platform.server.view.form.ActionObjectImplement;
import platform.server.view.form.ControlView;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ActionView;

public class ActionViewNavigator extends ControlViewNavigator<ActionInterface, Action, ActionObjectImplement, ActionObjectNavigator> {

    public ActionViewNavigator(int ID, ActionObjectNavigator view, GroupObjectNavigator toDraw) {
        super(ID, view, toDraw);
    }

    public ControlView createView(int ID, String sID, ActionObjectImplement implement, GroupObjectImplement toDraw) {
        return new ActionView(ID, sID, implement, toDraw);
    }
}

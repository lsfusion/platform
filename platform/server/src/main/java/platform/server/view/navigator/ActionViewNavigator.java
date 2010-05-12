package platform.server.view.navigator;

import platform.server.logics.control.ControlInterface;
import platform.server.logics.action.Action;
import platform.server.logics.action.ActionInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.view.form.ActionObjectImplement;
import platform.server.view.form.ControlView;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ActionView;

public class ActionViewNavigator<P extends ActionInterface> extends ControlViewNavigator<P, Action<P>, ActionObjectImplement<P>, ActionObjectNavigator<P>> {

    public ActionViewNavigator(int ID, ActionObjectNavigator<P> view, GroupObjectNavigator toDraw) {
        super(ID, view, toDraw);
    }

    public ControlView createView(int ID, String sID, ActionObjectImplement<P> implement, GroupObjectImplement toDraw) {
        return new ActionView<P>(ID, sID, implement, toDraw);
    }
}

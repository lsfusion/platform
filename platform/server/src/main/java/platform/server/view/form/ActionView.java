package platform.server.view.form;

import platform.server.logics.action.Action;
import platform.server.logics.action.ActionInterface;

public class ActionView<P extends ActionInterface> extends ControlView<P, Action<P>, ActionObjectImplement<P>> {

    public ActionView(int ID, String sID, ActionObjectImplement<P> view, GroupObjectImplement toDraw) {
        super(ID, sID, view, toDraw);
    }
}

package platform.server.view.form;

import platform.server.logics.action.Action;
import platform.server.logics.action.ActionInterface;

public class ActionView extends ControlView<ActionInterface, Action, ActionObjectImplement> {

    public ActionView(int ID, String sID, ActionObjectImplement view, GroupObjectImplement toDraw) {
        super(ID, sID, view, toDraw);
    }
}

package platform.fullclient.layout;

import bibliothek.gui.DockController;
import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;

class CloseAction extends SimpleButtonAction {

    CloseAction(DockController Controller) {
        setText("Close");
        setIcon(Controller.getIcons().getIcon("close"));
    }

    public void action(Dockable Form) {
        super.action(Form);
        DockStation Parent = Form.getDockParent();
        if(Parent!=null)
            Parent.drag(Form);

        // говорим о том что закрылись
        ((FormDockable)Form).closed();
    }
}

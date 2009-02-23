package platform.client.layout;

import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
import bibliothek.gui.DockStation;

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

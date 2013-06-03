package lsfusion.gwt.form.shared.view.actions;

import lsfusion.gwt.form.shared.view.GForm;

import java.io.IOException;

public class GDialogAction extends GExecuteAction {

    public GForm dialog;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GDialogAction() {}

    public GDialogAction(GForm dialog) {
        this.dialog = dialog;
    }

    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

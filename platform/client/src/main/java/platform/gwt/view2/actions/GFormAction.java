package platform.gwt.view2.actions;

import platform.gwt.view2.GForm;

import java.io.IOException;

public class GFormAction extends GExecuteAction {
    public boolean isModal;
    public GForm form;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFormAction() {}

    public GFormAction(boolean modal, GForm form) {
        isModal = modal;
        this.form = form;
    }

    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

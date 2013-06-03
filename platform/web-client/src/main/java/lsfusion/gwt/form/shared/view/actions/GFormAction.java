package lsfusion.gwt.form.shared.view.actions;

import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.window.GModalityType;

import java.io.IOException;

public class GFormAction extends GExecuteAction {
    public GModalityType modalityType;
    public GForm form;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFormAction() {}

    public GFormAction(GModalityType modalityType, GForm form) {
        this.modalityType = modalityType;
        this.form = form;
    }

    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.form.shared.view.actions;

import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.window.GModalityType;

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

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

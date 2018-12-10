package lsfusion.gwt.shared.view.actions;

import lsfusion.gwt.shared.form.view.GForm;
import lsfusion.gwt.shared.form.view.window.GModalityType;

public class GFormAction extends GExecuteAction {
    public GModalityType modalityType;
    public GForm form;
    public boolean forbidDuplicate;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFormAction() {}

    public GFormAction(GModalityType modalityType, GForm form, boolean forbidDuplicate) {
        this.modalityType = modalityType;
        this.form = form;
        this.forbidDuplicate = forbidDuplicate;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.client.action;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.navigator.window.GModalityType;

public class GFormAction extends GExecuteAction {
    public GModalityType modalityType;
    public GForm form;
    public boolean forbidDuplicate;
    public String formId;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFormAction() {}

    public GFormAction(GModalityType modalityType, GForm form, boolean forbidDuplicate, String formId) {
        this.modalityType = modalityType;
        this.form = form;
        this.forbidDuplicate = forbidDuplicate;
        this.formId = formId;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

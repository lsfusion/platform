package lsfusion.gwt.client.action;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.navigator.window.GShowFormType;

public class GFormAction extends GExecuteAction {
    public GShowFormType showFormType;
    public GForm form;
    public boolean forbidDuplicate;
    public String formId;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFormAction() {}

    public GFormAction(GShowFormType showFormType, GForm form, boolean forbidDuplicate, String formId) {
        this.showFormType = showFormType;
        this.form = form;
        this.forbidDuplicate = forbidDuplicate;
        this.formId = formId;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

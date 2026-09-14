package lsfusion.gwt.client.action;

public class GActivateFormAction extends GExecuteAction {
    // the address ACTIVATE FORM named: any part may be absent, and an absent part does not narrow it
    public String formCanonicalName;
    public String formId;
    public String windowCanonicalName;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GActivateFormAction() {
    }

    public GActivateFormAction(String formCanonicalName, String formId, String windowCanonicalName) {
        this.formCanonicalName = formCanonicalName;
        this.formId = formId;
        this.windowCanonicalName = windowCanonicalName;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.client.action;

public class GCloseFormAction extends GExecuteAction {
    // the address CLOSE FORM named: any part may be absent, and every form the address reaches is closed
    public String formId;
    public String formCanonicalName;
    public String windowCanonicalName;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GCloseFormAction() {
    }

    public GCloseFormAction(String formId, String formCanonicalName, String windowCanonicalName) {
        this.formId = formId;
        this.formCanonicalName = formCanonicalName;
        this.windowCanonicalName = windowCanonicalName;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

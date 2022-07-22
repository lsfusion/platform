package lsfusion.gwt.client.action;

public class GCloseFormAction extends GExecuteAction {
    public String formId;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GCloseFormAction() {
    }

    public GCloseFormAction(String formId) {
        this.formId = formId;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
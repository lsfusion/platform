package lsfusion.gwt.client.action;

public class GActivateFormAction extends GExecuteAction {
    public String formCanonicalName;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GActivateFormAction() {
    }

    public GActivateFormAction(String formCanonicalName) {
        this.formCanonicalName = formCanonicalName;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
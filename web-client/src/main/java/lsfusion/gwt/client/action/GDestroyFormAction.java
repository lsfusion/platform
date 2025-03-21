package lsfusion.gwt.client.action;

public class GDestroyFormAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GDestroyFormAction() {}

    public int closeDelay;

    public GDestroyFormAction(int closeDelay) {
        this.closeDelay = closeDelay;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

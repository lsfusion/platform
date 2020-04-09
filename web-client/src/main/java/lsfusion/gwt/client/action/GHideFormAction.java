package lsfusion.gwt.client.action;

public class GHideFormAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GHideFormAction() {}

    public int closeDelay;

    public GHideFormAction(int closeDelay) {
        this.closeDelay = closeDelay;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

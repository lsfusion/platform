package lsfusion.gwt.client.action;

public class GAsyncGetRemoteChangesAction extends GExecuteAction {
    public boolean forceLocalEvents;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GAsyncGetRemoteChangesAction() {}

    public GAsyncGetRemoteChangesAction(boolean forceLocalEvents) {
        this.forceLocalEvents = forceLocalEvents;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package lsfusion.interop.action;

public class AsyncGetRemoteChangesClientAction extends ExecuteClientAction {
    public boolean forceLocalEvents;

    public AsyncGetRemoteChangesClientAction(boolean forceLocalEvents) {
        this.forceLocalEvents = forceLocalEvents;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

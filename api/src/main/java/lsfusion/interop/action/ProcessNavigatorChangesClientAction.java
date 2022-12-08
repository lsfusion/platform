package lsfusion.interop.action;

public class ProcessNavigatorChangesClientAction extends ExecuteClientAction {
    public final long requestIndex;
    public final byte[] formChanges;

    public ProcessNavigatorChangesClientAction(long requestIndex, byte[] formChanges) {
        this.requestIndex = requestIndex;
        this.formChanges = formChanges;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

package lsfusion.interop.action;

public class ProcessNavigatorChangesClientAction extends ExecuteClientAction {
    public final long requestIndex;
    public final byte[] navigatorChanges;

    public ProcessNavigatorChangesClientAction(long requestIndex, byte[] navigatorChanges) {
        this.requestIndex = requestIndex;
        this.navigatorChanges = navigatorChanges;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

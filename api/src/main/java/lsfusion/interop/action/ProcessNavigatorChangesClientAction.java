package lsfusion.interop.action;

public class ProcessNavigatorChangesClientAction extends ExecuteClientAction {
    public final byte[] navigatorChanges;

    public ProcessNavigatorChangesClientAction(byte[] navigatorChanges) {
        this.navigatorChanges = navigatorChanges;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

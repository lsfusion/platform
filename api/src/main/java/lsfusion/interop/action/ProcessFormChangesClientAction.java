package lsfusion.interop.action;

import java.io.IOException;

public class ProcessFormChangesClientAction extends ExecuteClientAction {
    public final long requestIndex;
    public final byte[] formChanges;

    public ProcessFormChangesClientAction(long requestIndex, byte[] formChanges) {
        this.requestIndex = requestIndex;
        this.formChanges = formChanges;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

package platform.interop.action;

import java.io.IOException;

public class ProcessFormChangesClientAction extends ExecuteClientAction {
    public final long indexStamp;
    public final byte[] formChanges;

    public ProcessFormChangesClientAction(long indexStamp, byte[] formChanges) {
        this.indexStamp = indexStamp;
        this.formChanges = formChanges;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

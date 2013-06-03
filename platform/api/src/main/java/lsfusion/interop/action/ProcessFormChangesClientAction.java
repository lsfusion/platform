package lsfusion.interop.action;

import java.io.IOException;

public class ProcessFormChangesClientAction extends ExecuteClientAction {
    public final byte[] formChanges;

    public ProcessFormChangesClientAction(byte[] formChanges) {
        this.formChanges = formChanges;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

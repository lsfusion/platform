package lsfusion.interop.action;

import java.io.IOException;

public class UpdateEditValueClientAction extends ExecuteClientAction {

    public final byte[] value;

    public UpdateEditValueClientAction(byte[] value) {
        this.value = value;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

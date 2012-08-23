package platform.interop.action;

import java.io.IOException;

public class AsyncResultClientAction extends ExecuteClientAction {

    public final byte[] value;

    public AsyncResultClientAction(byte[] value) {
        this.value = value;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

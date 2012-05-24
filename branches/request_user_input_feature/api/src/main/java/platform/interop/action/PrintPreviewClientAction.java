package platform.interop.action;

import java.io.IOException;

public class PrintPreviewClientAction extends ExecuteClientAction {
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

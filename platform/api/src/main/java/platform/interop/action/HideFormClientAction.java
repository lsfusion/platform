package platform.interop.action;

import java.io.IOException;

public class HideFormClientAction extends ExecuteClientAction {

    public HideFormClientAction() {
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

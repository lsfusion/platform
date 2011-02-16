package platform.interop.action;

import java.io.IOException;

public class StopAutoActionsClientAction implements ClientAction {
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
    }

    public boolean isBeforeApply() {
        return false;
    }
}

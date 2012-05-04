package platform.interop.action;

import java.io.IOException;

public class DenyCloseFormClientAction implements ClientAction {
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    public boolean isBeforeApply() {
        return false;
    }
}

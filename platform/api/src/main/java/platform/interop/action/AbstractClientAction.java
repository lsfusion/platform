package platform.interop.action;

import java.io.IOException;

public abstract class AbstractClientAction implements ClientResultAction {

    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatchResult(dispatcher);
    }

    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException {
        dispatch(dispatcher);
        return true;
    }

    public boolean isBeforeApply() {
        return false;
    }
}

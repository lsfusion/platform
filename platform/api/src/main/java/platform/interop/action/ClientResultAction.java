package platform.interop.action;

import java.io.IOException;

public interface ClientResultAction extends ClientAction {
    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException;
}

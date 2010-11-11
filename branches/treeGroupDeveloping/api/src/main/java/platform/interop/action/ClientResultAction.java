package platform.interop.action;

import java.io.Serializable;
import java.io.IOException;

public interface ClientResultAction extends ClientApply {

    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException;
}

package platform.interop.action;

import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;
import java.io.Serializable;

public abstract class ClientAction extends ClientApply {

    protected ClientAction() {
    }

    public abstract Object dispatch(ClientActionDispatcher dispatcher) throws IOException;
}

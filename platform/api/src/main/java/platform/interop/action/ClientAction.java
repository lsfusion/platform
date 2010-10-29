package platform.interop.action;

import java.io.IOException;

public abstract class ClientAction extends ClientApply {

    protected ClientAction() {
    }

    public abstract Object dispatch(ClientActionDispatcher dispatcher) throws IOException;
}

package platform.interop.action;

import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;
import java.io.Serializable;

public abstract class ClientAction implements Serializable {

    public abstract void dispatch(ClientActionDispatcher dispatcher) throws IOException;
}

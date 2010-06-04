package platform.interop.action;

import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;
import java.io.Serializable;

public abstract class ClientAction<R extends ClientActionResult>  implements Serializable {

    public int ID = 0;
    public abstract R dispatch(ClientActionDispatcher dispatcher) throws IOException;
}

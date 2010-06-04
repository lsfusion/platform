package platform.interop.action;

import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;
import java.io.Serializable;

public abstract class ClientAction<R extends ClientActionResult>  implements Serializable {

    public int ID;

    protected ClientAction() {
        this(0);
    }

    protected ClientAction(int ID) {
        this.ID = ID;
    }

    public abstract R dispatch(ClientActionDispatcher dispatcher) throws IOException;
}

package platform.interop.action;

import java.io.IOException;

public abstract class CustomClientAction extends ClientAction {

    public abstract Object execute();

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

package platform.interop.action;

import java.io.IOException;

public abstract class CustomClientAction extends AbstractClientAction {

    public abstract Object execute();

    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

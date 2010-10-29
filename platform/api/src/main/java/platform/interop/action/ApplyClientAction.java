package platform.interop.action;

import java.io.IOException;

public class ApplyClientAction extends ClientAction {

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

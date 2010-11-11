package platform.interop.action;

import java.io.IOException;

public class UserChangedClientAction extends AbstractClientAction {

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

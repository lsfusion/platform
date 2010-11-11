package platform.interop.action;

import java.io.IOException;

public class ApplyClientAction extends AbstractClientAction {

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

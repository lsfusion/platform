package platform.interop.action;

import java.io.IOException;

public class CheckFailed extends AbstractClientAction {
    public String message;

    public CheckFailed(String message) {
        this.message = message;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        //do nothing
    }
}

package platform.interop.action;

import java.io.IOException;

public class CheckFailed  implements ClientAction {
    public String message;

    public CheckFailed(String message) {
        this.message = message;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return null;
    }
}

package platform.interop.action;

import java.io.IOException;

public class ResultClientAction extends ClientAction {

    public String message;

    public boolean failed;

    public ResultClientAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

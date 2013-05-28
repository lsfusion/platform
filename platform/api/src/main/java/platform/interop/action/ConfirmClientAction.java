package platform.interop.action;

import java.io.IOException;

public class ConfirmClientAction implements ClientAction {

    public String message;
    public String caption;

    public ConfirmClientAction(String caption, String message) {
        this.caption = caption;
        this.message = message;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

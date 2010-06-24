package platform.interop.action;

import java.io.IOException;

public class MessageClientAction extends ClientAction {

    public String message;
    public String caption;

    public MessageClientAction(String message, String caption) {
        this.message = message;
        this.caption = caption;
    }

    public ClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

package platform.interop.action;

import java.io.IOException;

public class MessageClientAction extends AbstractClientAction {

    public String message;
    public String caption;

    public MessageClientAction(String message, String caption) {
        this.message = message;
        this.caption = caption;
    }

    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

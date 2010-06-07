package platform.interop.action;

import java.io.IOException;

public class MessageFileClientAction extends ClientAction {

    public String fileName;

    public String caption;

    public MessageFileClientAction(String fileName, String caption) {
        this.fileName = fileName;
        this.caption = caption;
    }

    public ClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

package platform.interop.action;

import java.io.IOException;

public class MessageFileClientAction extends ClientAction {

    public String fileName;
    public String charsetName;

    public String caption;

    public int multiplier = 0;

    public MessageFileClientAction(String fileName, String charsetName, String caption, int multiplier) {
        this.fileName = fileName;
        this.charsetName = charsetName;
        this.caption = caption;
        this.multiplier = multiplier;
    }

    public ClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

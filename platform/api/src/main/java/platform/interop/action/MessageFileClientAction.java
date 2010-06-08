package platform.interop.action;

import java.io.IOException;

public class MessageFileClientAction extends ClientAction {

    public String fileName;
    public String charsetName;

    public boolean erase = false;

    public String caption;

    public int multiplier = 0;

    public MessageFileClientAction(String fileName, String charsetName, boolean erase, String caption, int multiplier) {
        this.fileName = fileName;
        this.charsetName = charsetName;
        this.erase = erase;
        this.caption = caption;
        this.multiplier = multiplier;
    }

    public ClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

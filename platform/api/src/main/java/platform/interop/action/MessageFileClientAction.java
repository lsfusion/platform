package platform.interop.action;

import java.io.IOException;

public class MessageFileClientAction implements ClientAction {

    public String fileName;
    public String charsetName;

    public boolean mustExist = false;
    public boolean erase = false;

    public String caption;

    public int multiplier = 0;

    public String mask;

    public MessageFileClientAction(String fileName, String charsetName, boolean mustExist, boolean erase, String caption) {
        this(fileName, charsetName, mustExist, erase, caption, 0);
    }

    public MessageFileClientAction(String fileName, String charsetName, boolean mustExist, boolean erase, String caption, int multiplier) {
        this(fileName, charsetName, mustExist, erase, caption, multiplier, null);
    }

    public MessageFileClientAction(String fileName, String charsetName, boolean mustExist, boolean erase, String caption, int multiplier, String mask) {
        this.fileName = fileName;
        this.charsetName = charsetName;
        this.mustExist = mustExist;
        this.erase = erase;
        this.caption = caption;
        this.multiplier = multiplier;
        this.mask = mask;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

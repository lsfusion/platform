package platform.interop.action;

import java.io.IOException;

public class ImportFileClientAction extends ClientAction {

    public String fileName;
    public String charsetName;

    public boolean erase = false;

    public ImportFileClientAction(String fileName, String charsetName, boolean erase) {
        this.fileName = fileName;
        this.charsetName = charsetName;
        this.erase = erase;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

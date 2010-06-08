package platform.interop.action;

import java.io.IOException;

public class ImportFileClientAction extends ClientAction<ImportFileClientActionResult> {

    public String fileName;
    public String charsetName;

    public boolean erase = false;

    public ImportFileClientAction(int ID, String fileName, String charsetName, boolean erase) {
        super(ID);
        this.fileName = fileName;
        this.charsetName = charsetName;
        this.erase = erase;
    }

    public ImportFileClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

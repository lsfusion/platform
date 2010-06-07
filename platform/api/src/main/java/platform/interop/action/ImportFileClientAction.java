package platform.interop.action;

import java.io.IOException;

public class ImportFileClientAction extends ClientAction<ImportFileClientActionResult> {

    public String fileName;
    public String charsetName;

    public ImportFileClientAction(int ID, String fileName, String charsetName) {
        super(ID);
        this.fileName = fileName;
        this.charsetName = charsetName;
    }

    public ImportFileClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

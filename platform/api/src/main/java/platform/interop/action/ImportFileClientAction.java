package platform.interop.action;

import java.io.IOException;

public class ImportFileClientAction extends ClientAction<ImportFileClientActionResult> {

    public String fileName;

    public ImportFileClientAction(String fileName) {
        this.fileName = fileName;
    }

    public ImportFileClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.executeImportFile(this);
    }
}

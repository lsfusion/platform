package platform.interop.action;

import java.io.IOException;

public class ExportFileClientAction extends ClientAction {

    public String fileName;
    public boolean append = false;
    public String fileText;

    public ExportFileClientAction(String fileName, boolean append, String fileText) {

        this.fileName = fileName;
        this.append = append;
        this.fileText = fileText;
    }

    public ClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.executeExportFile(this);
    }
}

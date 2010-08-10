package platform.interop.action;

import java.io.IOException;

public class ExportFileClientAction extends ClientAction {

    public String fileName;
    public boolean append = false;
    public String fileText;
    public String charsetName;

    public ExportFileClientAction(String fileName, boolean append, String fileText, String charsetName) {

        this.fileName = fileName;
        this.append = append;
        this.fileText = fileText;
        this.charsetName = charsetName;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

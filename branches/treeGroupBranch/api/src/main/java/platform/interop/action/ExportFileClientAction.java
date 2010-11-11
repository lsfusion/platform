package platform.interop.action;

import java.io.IOException;

public class ExportFileClientAction extends AbstractClientAction {

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

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.client.action.printer;

import lsfusion.gwt.client.action.GActionDispatcher;
import lsfusion.gwt.client.action.GExecuteAction;

public class GPrintFileAction extends GExecuteAction {
    public String fileData;
    public String filePath;
    public String printerName;

    public GPrintFileAction() {}

    public GPrintFileAction(String fileData, String filePath, String printerName) {
        this.fileData = fileData;
        this.filePath = filePath;
        this.printerName = printerName;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
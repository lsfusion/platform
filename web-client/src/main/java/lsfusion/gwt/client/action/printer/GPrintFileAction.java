package lsfusion.gwt.client.action.printer;

import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GActionDispatcher;
import lsfusion.gwt.client.action.GExecuteAction;

public class GPrintFileAction implements GAction {
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
    public String dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
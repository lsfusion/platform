package lsfusion.gwt.client.action.file;

import lsfusion.gwt.client.action.GActionDispatcher;
import lsfusion.gwt.client.action.GExecuteAction;

public class GWriteAction extends GExecuteAction {
    public String fileUrl;
    public String filePath;
    public String fileData;

    @SuppressWarnings("UnusedDeclaration")
    public GWriteAction() {}

    public GWriteAction(String fileUrl, String filePath, String fileData) {
        this.fileUrl = fileUrl;
        this.filePath = filePath;
        this.fileData = fileData;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

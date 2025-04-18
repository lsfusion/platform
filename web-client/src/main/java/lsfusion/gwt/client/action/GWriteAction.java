package lsfusion.gwt.client.action;

public class GWriteAction extends GExecuteAction {
    public String fileUrl;
    public String filePath;
    public String fileBase64;

    @SuppressWarnings("UnusedDeclaration")
    public GWriteAction() {}

    public GWriteAction(String fileUrl, String filePath, String fileBase64) {
        this.fileUrl = fileUrl;
        this.filePath = filePath;
        this.fileBase64 = fileBase64;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

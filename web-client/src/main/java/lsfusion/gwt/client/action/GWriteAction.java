package lsfusion.gwt.client.action;

public class GWriteAction extends GExecuteAction {
    public String fileUrl;
    public String filePath;

    @SuppressWarnings("UnusedDeclaration")
    public GWriteAction() {}

    public GWriteAction(String fileUrl, String filePath) {
        this.fileUrl = fileUrl;
        this.filePath = filePath;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.client.action;

public class GWriteAction extends GExecuteAction {
    public String fileUrl;

    @SuppressWarnings("UnusedDeclaration")
    public GWriteAction() {}

    public GWriteAction(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

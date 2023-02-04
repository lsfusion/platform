package lsfusion.gwt.client.action;

public class GOpenFileAction extends GExecuteAction {
    public String fileUrl;

    @SuppressWarnings("UnusedDeclaration")
    public GOpenFileAction() {}

    public GOpenFileAction(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

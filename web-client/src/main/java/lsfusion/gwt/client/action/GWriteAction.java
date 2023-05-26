package lsfusion.gwt.client.action;

public class GWriteAction extends GExecuteAction {

    public String fileName;
    public String displayName;
    public String extension;

    @SuppressWarnings("UnusedDeclaration")
    public GWriteAction() {}

    public GWriteAction(String fileName, String displayName, String extension) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.extension = extension;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

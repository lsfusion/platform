package lsfusion.gwt.shared.view.actions;

public class GOpenFileAction extends GExecuteAction {
    public String filePath;
    public String fileName;

    @SuppressWarnings("UnusedDeclaration")
    public GOpenFileAction() {}

    public GOpenFileAction(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

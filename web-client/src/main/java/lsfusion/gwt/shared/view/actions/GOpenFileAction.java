package lsfusion.gwt.shared.view.actions;

public class GOpenFileAction extends GExecuteAction {
    public String fileName;
    public String displayName;
    public String extension;

    @SuppressWarnings("UnusedDeclaration")
    public GOpenFileAction() {}

    public GOpenFileAction(String fileName, String displayName, String extension) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.extension = extension;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

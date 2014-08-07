package lsfusion.gwt.form.shared.view.actions;

public class GOpenFileAction extends GExecuteAction {
    public String filePath;

    @SuppressWarnings("UnusedDeclaration")
    public GOpenFileAction() {}

    public GOpenFileAction(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

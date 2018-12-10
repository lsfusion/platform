package lsfusion.gwt.shared.form.actions;

public class GBeepAction extends GExecuteAction {
    public String filePath;
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GBeepAction() {}

    public GBeepAction(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

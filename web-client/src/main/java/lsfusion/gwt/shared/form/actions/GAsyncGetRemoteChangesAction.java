package lsfusion.gwt.shared.form.actions;

public class GAsyncGetRemoteChangesAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GAsyncGetRemoteChangesAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

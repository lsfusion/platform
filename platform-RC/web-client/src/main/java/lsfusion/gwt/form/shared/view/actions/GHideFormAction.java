package lsfusion.gwt.form.shared.view.actions;

public class GHideFormAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GHideFormAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

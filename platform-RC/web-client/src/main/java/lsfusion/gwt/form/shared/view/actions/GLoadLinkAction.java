package lsfusion.gwt.form.shared.view.actions;

public class GLoadLinkAction extends GExecuteAction {

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GLoadLinkAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
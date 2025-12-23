package lsfusion.gwt.client.action;

public class GHideFormAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GHideFormAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(GActionDispatcher dispatcher, GActionDispatcherLookAhead lookAhead) throws Throwable {
        dispatcher.execute(this, lookAhead);
    }
}

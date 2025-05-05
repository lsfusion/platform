package lsfusion.gwt.client.action;

public class GGetAvailablePrintersAction implements GAction {
    public GGetAvailablePrintersAction() {}

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
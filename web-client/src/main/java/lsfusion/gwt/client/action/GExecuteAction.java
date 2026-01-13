package lsfusion.gwt.client.action;

public abstract class GExecuteAction implements GAction {

    public final Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        execute(dispatcher);
        return null;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher, GActionDispatcherLookAhead lookAhead) throws Throwable {
        execute(dispatcher, lookAhead);
        return null;
    }

    public abstract void execute(GActionDispatcher dispatcher) throws Throwable;

    public void execute(GActionDispatcher dispatcher, GActionDispatcherLookAhead lookAhead) throws Throwable {
        execute(dispatcher);
    }
}

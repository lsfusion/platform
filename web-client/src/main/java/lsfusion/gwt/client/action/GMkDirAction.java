package lsfusion.gwt.client.action;

public class GMkDirAction implements GAction {
    public String source;

    @SuppressWarnings("UnusedDeclaration")
    public GMkDirAction() {}

    public GMkDirAction(String source) {
        this.source = source;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
package lsfusion.gwt.client.action;

public class GMoveFileAction implements GAction {
    public String source;
    public String destination;

    @SuppressWarnings("UnusedDeclaration")
    public GMoveFileAction() {}

    public GMoveFileAction(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
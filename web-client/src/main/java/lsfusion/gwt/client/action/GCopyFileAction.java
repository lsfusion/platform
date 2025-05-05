package lsfusion.gwt.client.action;

public class GCopyFileAction implements GAction {
    public String source;
    public String destination;

    @SuppressWarnings("UnusedDeclaration")
    public GCopyFileAction() {}

    public GCopyFileAction(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
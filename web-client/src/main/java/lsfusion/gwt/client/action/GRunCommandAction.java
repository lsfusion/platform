package lsfusion.gwt.client.action;

public class GRunCommandAction implements GAction {
    public String command;

    @SuppressWarnings("UnusedDeclaration")
    public GRunCommandAction() {}

    public GRunCommandAction(String command) {
        this.command = command;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}

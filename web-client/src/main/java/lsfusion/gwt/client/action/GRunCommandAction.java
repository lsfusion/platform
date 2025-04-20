package lsfusion.gwt.client.action;

public class GRunCommandAction extends GExecuteAction {
    public String command;

    @SuppressWarnings("UnusedDeclaration")
    public GRunCommandAction() {}

    public GRunCommandAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

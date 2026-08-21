package lsfusion.gwt.client.action;

public class GRunCommandAction implements GAction {
    public String command;
    public String directory;
    public boolean wait;

    @SuppressWarnings("UnusedDeclaration")
    public GRunCommandAction() {}

    public GRunCommandAction(String command, String directory, boolean wait) {
        this.command = command;
        this.directory = directory;
        this.wait = wait;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}

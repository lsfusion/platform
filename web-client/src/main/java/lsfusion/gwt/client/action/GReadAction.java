package lsfusion.gwt.client.action;

public class GReadAction extends GExecuteAction {
    public String sourcePath;

    @SuppressWarnings("UnusedDeclaration")
    public GReadAction() {}

    public GReadAction(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.client.action;

public class GOpenUriAction extends GExecuteAction {
    public String path;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GOpenUriAction() {}

    public GOpenUriAction(String path) {
        this.path = path;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

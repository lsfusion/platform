package lsfusion.gwt.client.action;

public class GMessageAction extends GExecuteAction {
    public String message;
    public String caption;

    public boolean syncType;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GMessageAction() {}

    public GMessageAction(String message, String caption, boolean syncType) {
        this.message = message;
        this.caption = caption;

        this.syncType = syncType;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.client.action;

public class GResetFilterGroupAction extends GExecuteAction {
    public String sid;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GResetFilterGroupAction() {
    }

    public GResetFilterGroupAction(String sid) {
        this.sid = sid;
    }
    
    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
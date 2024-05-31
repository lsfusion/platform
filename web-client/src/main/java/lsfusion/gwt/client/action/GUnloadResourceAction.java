package lsfusion.gwt.client.action;

public class GUnloadResourceAction extends GExecuteAction {
    public String resource;

    public GUnloadResourceAction() {
    }

    public GUnloadResourceAction(String resource) {
        this.resource = resource;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
package lsfusion.gwt.client.action;

public class GLoadResourceAction extends GExecuteAction {
    public String path;
    public String extension;

    public GLoadResourceAction() {
    }

    public GLoadResourceAction(String path, String extension) {
        this.path = path;
        this.extension = extension;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
package lsfusion.interop.action;

public class LoadLinkClientAction implements ClientAction {
    public boolean directory;

    public LoadLinkClientAction(boolean directory) {
        this.directory = directory;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}
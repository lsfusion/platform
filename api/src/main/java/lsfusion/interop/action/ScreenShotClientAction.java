package lsfusion.interop.action;

public class ScreenShotClientAction implements ClientAction {

    public final boolean html;
    public final String containerSID;

    public ScreenShotClientAction(boolean html, String containerSID) {
        this.html = html;
        this.containerSID = containerSID;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}

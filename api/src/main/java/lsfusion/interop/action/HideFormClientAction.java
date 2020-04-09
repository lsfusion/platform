package lsfusion.interop.action;

public class HideFormClientAction extends ExecuteClientAction {

    public final int closeConfirmedDelay;
    public final int closeNotConfirmedDelay;

    public HideFormClientAction(int closeConfirmedDelay, int closeNotConfirmedDelay) {
        this.closeConfirmedDelay = closeConfirmedDelay;
        this.closeNotConfirmedDelay = closeNotConfirmedDelay;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

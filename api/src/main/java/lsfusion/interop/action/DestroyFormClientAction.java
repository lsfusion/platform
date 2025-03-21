package lsfusion.interop.action;

import java.io.IOException;

public class DestroyFormClientAction extends ExecuteClientAction {

    public final int closeConfirmedDelay;
    public final int closeNotConfirmedDelay;

    public DestroyFormClientAction(int closeConfirmedDelay, int closeNotConfirmedDelay) {
        this.closeConfirmedDelay = closeConfirmedDelay;
        this.closeNotConfirmedDelay = closeNotConfirmedDelay;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

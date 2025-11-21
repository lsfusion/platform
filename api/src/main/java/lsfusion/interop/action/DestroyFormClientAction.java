package lsfusion.interop.action;

import java.io.IOException;

public class DestroyFormClientAction extends ExecuteClientAction {

    public final int closeConfirmedDelay;
    public final int closeNotConfirmedDelay;
    public boolean keepRemoteForm;

    public DestroyFormClientAction(int closeConfirmedDelay, int closeNotConfirmedDelay, boolean keepRemoteForm) {
        this.closeConfirmedDelay = closeConfirmedDelay;
        this.closeNotConfirmedDelay = closeNotConfirmedDelay;
        this.keepRemoteForm = keepRemoteForm;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

package lsfusion.interop.action;

import java.io.IOException;

public class HideFormClientAction extends ExecuteClientAction {

    public final int closeFormDelay;

    public HideFormClientAction(int closeFormDelay) {
        this.closeFormDelay = closeFormDelay;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

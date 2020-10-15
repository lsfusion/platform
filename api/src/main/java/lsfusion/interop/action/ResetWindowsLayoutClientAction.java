package lsfusion.interop.action;

import java.io.IOException;

public class ResetWindowsLayoutClientAction extends ExecuteClientAction {
    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

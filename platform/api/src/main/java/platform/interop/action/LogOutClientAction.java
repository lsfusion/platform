package platform.interop.action;

import java.io.IOException;

public class LogOutClientAction extends ExecuteClientAction {
    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

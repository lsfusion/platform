package lsfusion.interop.action;

import java.io.IOException;

public class LogOutClientAction extends ExecuteClientAction {
    public boolean restart;
    public boolean reconnect;

    public LogOutClientAction(boolean restart, boolean reconnect) {
        this.restart = restart;
        this.reconnect = reconnect;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

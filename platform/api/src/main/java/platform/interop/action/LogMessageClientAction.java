package platform.interop.action;

import java.io.IOException;

public class LogMessageClientAction extends ExecuteClientAction {

    public boolean failed;
    public String message;

    public LogMessageClientAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

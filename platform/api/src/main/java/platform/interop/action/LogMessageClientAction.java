package platform.interop.action;

import java.io.IOException;

public class LogMessageClientAction extends AbstractClientAction {

    public String message;

    public boolean failed;

    public LogMessageClientAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public boolean isBeforeApply() {
        return true;
    }
}

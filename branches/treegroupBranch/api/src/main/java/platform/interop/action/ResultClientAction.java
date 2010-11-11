package platform.interop.action;

import java.io.IOException;

public class ResultClientAction extends AbstractClientAction {

    public String message;

    public boolean failed;

    public ResultClientAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    @Override
    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }

    @Override
    public boolean isBeforeApply() {
        return true;
    }
}

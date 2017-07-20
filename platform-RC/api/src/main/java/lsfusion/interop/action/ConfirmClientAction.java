package lsfusion.interop.action;

import java.io.IOException;

public class ConfirmClientAction implements ClientAction {

    public String message;
    public String caption;
    public boolean cancel;
    public int timeout;
    public int initialValue;

    public ConfirmClientAction(String caption, String message) {
        this(caption, message, false, 0, 0);
    }

    public ConfirmClientAction(String caption, String message, boolean cancel, int timeout, int initialValue) {
        this.caption = caption;
        this.message = message;
        this.cancel = cancel;
        this.timeout = timeout;
        this.initialValue = initialValue;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "ConfirmClientAction[caption: " + caption + ", msg: " + message + "]";
    }
}

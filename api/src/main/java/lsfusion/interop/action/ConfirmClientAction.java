package lsfusion.interop.action;

import java.io.IOException;

public class ConfirmClientAction implements ClientAction {

    public String message;
    public String caption;
    public boolean cancel;

    public ConfirmClientAction(String caption, String message) {
        this(caption, message, false);
    }

    public ConfirmClientAction(String caption, String message, boolean cancel) {
        this.caption = caption;
        this.message = message;
        this.cancel = cancel;
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

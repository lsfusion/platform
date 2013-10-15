package lsfusion.interop.action;

import java.io.IOException;

public class MessageClientAction extends ExecuteClientAction {

    public String message;
    public String caption;
    public boolean extended;

    public MessageClientAction(String message, String caption) {
        this(message, caption, false);
    }

    public MessageClientAction(String message, String caption, boolean extended) {
        this.message = message;
        this.caption = caption;
        this.extended = extended;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "MessageClientAction[caption: " + caption + ", msg: " + message + ", ext: " + extended + "]";
    }
}

package lsfusion.interop.action;

public class MessageClientAction extends ExecuteClientAction {

    public String message;
    public String caption;
    public boolean extended;

    public boolean syncType;

    public MessageClientAction(String message, String caption) {
        this(message, caption, false);
    }

    public MessageClientAction(String message, String caption, boolean extended) {
        this(message, caption, extended, false);
    }

    public MessageClientAction(String message, String caption, boolean extended, boolean syncType) {
        this.message = message;
        this.caption = caption;
        this.extended = extended;

        this.syncType = syncType;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "MessageClientAction[caption: " + caption + ", msg: " + message + ", ext: " + extended + "]";
    }
}

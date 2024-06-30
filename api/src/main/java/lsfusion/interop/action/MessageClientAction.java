package lsfusion.interop.action;

import java.util.ArrayList;
import java.util.List;

public class MessageClientAction extends ExecuteClientAction {

    public String message;
    public String textMessage;
    public String caption;

    public List<List<String>> data;
    public List<String> titles;

    public MessageClientType type;
    public boolean syncType;

    // message method should be used instead
    @Deprecated
    public MessageClientAction(String message, String caption) {
        this(message, message, caption, new ArrayList<>(), new ArrayList<>(), MessageClientType.SYSTEM(false), false);
    }

    public MessageClientAction(String message, String textMessage, String caption, List<List<String>> data, List<String> titles, MessageClientType type, boolean syncType) {
        this.message = message;
        this.textMessage = textMessage;
        this.caption = caption;

        this.data = data;
        this.titles = titles;

        this.type = type;
        this.syncType = syncType;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "MessageClientAction[caption: " + caption + ", msg: " + message + ", type: " + type + "]";
    }
}

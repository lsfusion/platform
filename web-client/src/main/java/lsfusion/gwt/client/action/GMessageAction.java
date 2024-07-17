package lsfusion.gwt.client.action;

import java.io.Serializable;
import java.util.ArrayList;

public class GMessageAction extends GExecuteAction {
    public Serializable message;
    public String textMessage;
    public String caption;

    public ArrayList<ArrayList<String>> data;
    public ArrayList<String> titles;

    public GMessageType type;
    public boolean syncType;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GMessageAction() {}

    public GMessageAction(Serializable message, String textMessage, String caption, ArrayList<ArrayList<String>> data, ArrayList<String> titles, GMessageType type, boolean syncType) {
        this.message = message;
        this.textMessage = textMessage;
        this.caption = caption;

        this.data = data;
        this.titles = titles;

        this.type = type;
        this.syncType = syncType;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

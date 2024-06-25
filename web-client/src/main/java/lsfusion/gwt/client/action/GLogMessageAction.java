package lsfusion.gwt.client.action;

import java.util.ArrayList;

public class GLogMessageAction extends GExecuteAction {
    public boolean failed;
    public String message;
    public ArrayList<ArrayList<String>> data;
    public ArrayList<String> titles;

    public boolean syncType;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GLogMessageAction() {}

    public GLogMessageAction(boolean failed, String message, ArrayList<ArrayList<String>> data, ArrayList<String> titles, boolean syncType) {
        this.failed = failed;
        this.message = message;
        this.data = data;
        this.titles = titles;

        this.syncType = syncType;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

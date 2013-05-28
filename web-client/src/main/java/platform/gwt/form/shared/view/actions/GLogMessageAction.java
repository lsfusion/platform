package platform.gwt.form.shared.view.actions;

import java.io.IOException;
import java.util.ArrayList;

public class GLogMessageAction extends GExecuteAction {
    public boolean failed;
    public String message;
    public ArrayList<ArrayList<String>> data;
    public ArrayList<String> titles;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GLogMessageAction() {}

    public GLogMessageAction(boolean failed, String message, ArrayList<ArrayList<String>> data, ArrayList<String> titles) {
        this.failed = failed;
        this.message = message;
        this.data = data;
        this.titles = titles;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

package platform.interop.action;

import java.io.IOException;
import java.util.ArrayList;

public class LogMessageClientAction extends ExecuteClientAction {

    public boolean failed;
    public String message;
    public ArrayList<ArrayList<String>> data;
    public ArrayList<String> titles;
    public String textMessage;

    public LogMessageClientAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    public LogMessageClientAction(String textMessage, ArrayList<String> titles, ArrayList<ArrayList<String>> data, boolean failed) {
        this.textMessage = textMessage;
        this.titles = titles;
        this.data = data;
        this.failed = failed;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

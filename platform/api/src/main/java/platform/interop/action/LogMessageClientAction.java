package platform.interop.action;

import java.io.IOException;
import java.util.ArrayList;

public class LogMessageClientAction extends ExecuteClientAction {

    public boolean failed;
    public String message;
    public ArrayList<ArrayList<String>> data;
    public ArrayList<String> titles;

    public LogMessageClientAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    public LogMessageClientAction(String message, ArrayList<String> titles, ArrayList<ArrayList<String>> data, boolean failed) {
        this.message = message;
        this.titles = titles;
        this.data = data;
        this.failed = failed;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

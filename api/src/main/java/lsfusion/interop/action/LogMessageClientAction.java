package lsfusion.interop.action;

import java.util.ArrayList;
import java.util.List;

public class LogMessageClientAction extends ExecuteClientAction {

    public boolean failed;
    public String message;
    public List<List<String>> data;
    public List<String> titles;

    public LogMessageClientAction(String message, boolean failed) {
        this(message, new ArrayList<>(), new ArrayList<>(), failed);
    }

    public LogMessageClientAction(String message, List<String> titles, List<List<String>> data, boolean failed) {
        this.message = message;
        this.titles = titles;
        this.data = data;
        this.failed = failed;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "LogMessageClientAction[msg: " + message + ", data: " + data + "]";
    }
}

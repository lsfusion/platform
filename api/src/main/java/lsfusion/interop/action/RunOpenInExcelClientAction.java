package lsfusion.interop.action;

import java.io.IOException;

public class RunOpenInExcelClientAction extends ExecuteClientAction {
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

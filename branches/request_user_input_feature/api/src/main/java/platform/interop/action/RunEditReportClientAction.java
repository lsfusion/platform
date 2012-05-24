package platform.interop.action;

import java.io.IOException;

public class RunEditReportClientAction extends ExecuteClientAction {
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

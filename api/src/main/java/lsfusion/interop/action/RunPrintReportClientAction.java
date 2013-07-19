package lsfusion.interop.action;

import java.io.IOException;

public class RunPrintReportClientAction extends ExecuteClientAction {
    public boolean isDebug;

    public RunPrintReportClientAction(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

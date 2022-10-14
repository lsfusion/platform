package lsfusion.interop.action;

import java.util.List;

public class RunEditReportClientAction extends ExecuteClientAction {
    
    public final List<String> customReportPathList;
    
    public RunEditReportClientAction(List<String> customReportPathList) {
        this.customReportPathList = customReportPathList;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

package lsfusion.interop.action;

import java.io.IOException;
import java.util.List;

public class RunEditReportClientAction extends ExecuteClientAction {
    
    public final List<ReportPath> customReportPathList;
    
    public RunEditReportClientAction(List<ReportPath> customReportPathList) {
        this.customReportPathList = customReportPathList;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}

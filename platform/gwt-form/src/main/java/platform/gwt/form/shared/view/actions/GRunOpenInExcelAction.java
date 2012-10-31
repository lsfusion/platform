package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GRunOpenInExcelAction extends GExecuteAction {
    private String reportSID;
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRunOpenInExcelAction() {}

    public GRunOpenInExcelAction(String reportSID) {
        this.reportSID = reportSID;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    public String getReportSID() {
        return reportSID;
    }
}

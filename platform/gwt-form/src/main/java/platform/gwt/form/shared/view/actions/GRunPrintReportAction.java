package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GRunPrintReportAction extends GExecuteAction {
    private String reportSID;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRunPrintReportAction() {}

    public GRunPrintReportAction(String reportSID) {
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

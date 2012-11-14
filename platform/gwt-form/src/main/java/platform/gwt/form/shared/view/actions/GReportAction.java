package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GReportAction extends GExecuteAction {
    public String reportSID;
    public boolean isModal;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public GReportAction(String reportSID) {
        this.reportSID = reportSID;
    }

    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

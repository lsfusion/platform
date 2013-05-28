package platform.interop.action;

import platform.interop.form.ReportGenerationData;

import java.io.IOException;

public class ReportClientAction extends ExecuteClientAction {

    public String reportSID;
    public boolean isModal;
    public ReportGenerationData generationData;

    public ReportClientAction(String reportSID, boolean isModal, ReportGenerationData generationData) {
        this.reportSID = reportSID;
        this.isModal = isModal;
        this.generationData = generationData;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

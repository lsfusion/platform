package lsfusion.interop.action;

import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;

public class ReportClientAction extends ExecuteClientAction {

    public String reportSID;
    public boolean isModal;
    public ReportGenerationData generationData;
    public boolean isDebug;
    public FormPrintType printType;

    public ReportClientAction(String reportSID, boolean isModal, ReportGenerationData generationData, FormPrintType printType, boolean isDebug) {
        this.reportSID = reportSID;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
        this.isDebug = isDebug;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}

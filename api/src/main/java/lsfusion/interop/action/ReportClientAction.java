package lsfusion.interop.action;

import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;
import java.util.Map;

public class ReportClientAction implements ClientAction {

    public Map<String, String> reportPath;
    public boolean isModal;
    public ReportGenerationData generationData;
    public boolean isDebug;
    public FormPrintType printType;

    public ReportClientAction(Map<String, String> reportPath, boolean isModal, ReportGenerationData generationData, FormPrintType printType, boolean isDebug) {
        this.reportPath = reportPath;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
        this.isDebug = isDebug;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

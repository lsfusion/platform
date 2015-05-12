package lsfusion.interop.action;

import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;

public class ReportClientAction implements ClientAction {

    public String formSID;
    public boolean isModal;
    public ReportGenerationData generationData;
    public boolean isDebug;
    public FormPrintType printType;

    public ReportClientAction(String formSID, boolean isModal, ReportGenerationData generationData, FormPrintType printType, boolean isDebug) {
        this.formSID = formSID;
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

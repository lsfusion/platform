package lsfusion.interop.action;

import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;
import java.util.List;

public class ReportClientAction implements ClientAction {

    public List<ReportPath> reportPathList;
    public String formSID;
    public boolean isModal;
    public ReportGenerationData generationData;
    public boolean inDevMode;
    public FormPrintType printType;
    public String printerName;
    public String password;
    public String sheetName;

    public ReportClientAction(List<ReportPath> reportPathList, String formSID, boolean isModal, ReportGenerationData generationData,
                              FormPrintType printType, String printerName, boolean inDevMode, String password, String sheetName) {
        this.reportPathList = reportPathList;
        this.formSID = formSID;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
        this.printerName = printerName;
        this.inDevMode = inDevMode;
        this.password = password;
        this.sheetName = sheetName;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

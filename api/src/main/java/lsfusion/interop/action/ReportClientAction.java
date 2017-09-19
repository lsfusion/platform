package lsfusion.interop.action;

import lsfusion.interop.FormPrintType;
import lsfusion.interop.form.ReportGenerationData;

import java.io.IOException;
import java.util.List;

public class ReportClientAction implements ClientAction {

    public List<ReportPath> reportPathList;
    public List<ReportPath> autoReportPathList;
    public boolean isModal;
    public ReportGenerationData generationData;
    public boolean isDebug;
    public FormPrintType printType;
    public String printerName;

    public ReportClientAction(List<ReportPath> reportPathList, List<ReportPath> autoReportPathList, boolean isModal, ReportGenerationData generationData,
                              FormPrintType printType, String printerName, boolean isDebug) {
        this.reportPathList = reportPathList;
        this.autoReportPathList = autoReportPathList;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
        this.printerName = printerName;
        this.isDebug = isDebug;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}

package lsfusion.interop.action;

import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;

import java.util.List;

public class ReportClientAction implements ClientAction {

    public List<String> reportPathList;
    public String formSID;
    public String formCaption;
    public boolean autoPrint;
    public boolean isModal;
    public ReportGenerationData generationData;
    public boolean inDevMode;
    public FormPrintType printType;
    public String printerName;
    public String password;
    public String sheetName;

    public ReportClientAction(List<String> reportPathList, String formCaption, String formSID, boolean autoPrint, boolean isModal, ReportGenerationData generationData,
                              FormPrintType printType, String printerName, boolean inDevMode, String password, String sheetName) {
        this.reportPathList = reportPathList;
        this.formCaption = formCaption;
        this.formSID = formSID;
        this.autoPrint = autoPrint;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
        this.printerName = printerName;
        this.inDevMode = inDevMode;
        this.password = password;
        this.sheetName = sheetName;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}

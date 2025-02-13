package lsfusion.interop.action;

import lsfusion.base.file.FileData;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;

import java.util.List;

public class ReportClientAction implements ClientAction {

    // ALL
    public boolean autoPrint;

    // WEB
    public Integer autoPrintTimeout;
    public FileData fileData;

    // DESKTOP
    public ReportGenerationData generationData;
    public FormPrintType printType;
    public List<String> reportPathList;
    public String formSID;
    public String formCaption;
    public boolean isModal;
    public boolean inDevMode;
    public String printerName;
    public boolean useDefaultPrinterInPrintIfNotSpecified;
    public String password;
    public String sheetName;
    public boolean jasperReportsIgnorePageMargins;

    public ReportClientAction(boolean autoPrint, Integer autoPrintTimeout, FileData fileData) {
        this.autoPrint = autoPrint;

        this.autoPrintTimeout = autoPrintTimeout;
        this.fileData = fileData;
    }

    public ReportClientAction(List<String> reportPathList, String formCaption, String formSID, boolean autoPrint, boolean isModal, ReportGenerationData generationData,
                              FormPrintType printType, String printerName, boolean useDefaultPrinterInPrintIfNotSpecified, boolean inDevMode, String password, String sheetName, boolean jasperReportsIgnorePageMargins) {
        this.autoPrint = autoPrint;

        this.reportPathList = reportPathList;
        this.formCaption = formCaption;
        this.formSID = formSID;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
        this.printerName = printerName;
        this.useDefaultPrinterInPrintIfNotSpecified = useDefaultPrinterInPrintIfNotSpecified;
        this.inDevMode = inDevMode;
        this.password = password;
        this.sheetName = sheetName;
        this.jasperReportsIgnorePageMargins = jasperReportsIgnorePageMargins;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}

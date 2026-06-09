package lsfusion.interop.action;

import lsfusion.base.file.FileData;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;

import java.util.List;

public class ReportClientAction implements ClientAction {

    // ALL
    public boolean autoPrint;
    public String formCaption;
    public String printerName;

    // WEB
    public Integer autoPrintTimeout;
    public FileData fileData;

    // DESKTOP
    public ReportGenerationData generationData;
    public FormPrintType printType;
    public List<String> reportPathList;
    public String formSID;
    public boolean isModal;
    public boolean inDevMode;
    public boolean useDefaultPrinterInPrintIfNotSpecified;
    public String password;
    public String sheetName;
    public boolean jasperReportsIgnorePageMargins;

    public ReportClientAction(boolean autoPrint, String formCaption, String printerName, Integer autoPrintTimeout, FileData fileData) {
        this.autoPrint = autoPrint;
        this.formCaption = formCaption;
        this.printerName = printerName;

        this.autoPrintTimeout = autoPrintTimeout;
        this.fileData = fileData;
    }

    public ReportClientAction(boolean autoPrint, String formCaption, String printerName, List<String> reportPathList, String formSID, boolean isModal, ReportGenerationData generationData,
                              FormPrintType printType, boolean useDefaultPrinterInPrintIfNotSpecified, boolean inDevMode, String password, String sheetName, boolean jasperReportsIgnorePageMargins) {
        this.autoPrint = autoPrint;
        this.formCaption = formCaption;
        this.printerName = printerName;

        this.reportPathList = reportPathList;
        this.formSID = formSID;
        this.isModal = isModal;
        this.generationData = generationData;
        this.printType = printType;
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

package lsfusion.gwt.client.action;

public class GReportAction extends GExecuteAction {
    public String reportFileName;
    public boolean autoPrint;
    public Integer autoPrintTimeout;
    public String printerName;
    public String fileData;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public GReportAction(String reportFileName, boolean autoPrint, Integer autoPrintTimeout, String printerName, String fileData) {
        this.reportFileName = reportFileName;
        this.autoPrint = autoPrint;
        this.autoPrintTimeout = autoPrintTimeout;
        this.printerName = printerName;
        this.fileData = fileData;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

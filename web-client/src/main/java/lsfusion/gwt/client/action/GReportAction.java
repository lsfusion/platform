package lsfusion.gwt.client.action;

public class GReportAction extends GExecuteAction {
    public String reportFileName;
    public boolean autoPrint;
    public Integer autoPrintTimeout;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public GReportAction(String reportFileName, boolean autoPrint, Integer autoPrintTimeout) {
        this.reportFileName = reportFileName;
        this.autoPrint = autoPrint;
        this.autoPrintTimeout = autoPrintTimeout;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

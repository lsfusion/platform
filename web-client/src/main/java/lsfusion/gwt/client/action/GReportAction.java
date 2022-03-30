package lsfusion.gwt.client.action;

public class GReportAction extends GExecuteAction {
    public String reportFileName;
    public String reportExtension;
    public boolean isAutoPrint;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public GReportAction(String reportFileName, String reportExtension, boolean isAutoPrint) {
        this.reportFileName = reportFileName;
        this.reportExtension = reportExtension;
        this.isAutoPrint = isAutoPrint;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

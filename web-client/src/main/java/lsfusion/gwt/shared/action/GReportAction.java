package lsfusion.gwt.shared.action;

public class GReportAction extends GExecuteAction {
    public String reportFileName;
    public String reportExtension;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public GReportAction(String reportFileName, String reportExtension) {
        this.reportFileName = reportFileName;
        this.reportExtension = reportExtension;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package lsfusion.gwt.form.shared.view.actions;

public class GReportAction extends GExecuteAction {
    public String reportFileName;
    public boolean isModal;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public GReportAction(String reportFileName) {
        this.reportFileName = reportFileName;
    }

    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}

package platform.gwt.form.shared.view.actions;

public interface GActionDispatcher {
    public void execute(GFormAction action);

    public void execute(GReportAction action);

    public void execute(GDialogAction action);

    public Object execute(GChooseClassAction action);

    public void execute(GMessageAction action);

    public int execute(GConfirmAction action);

    public void execute(GLogMessageAction action);

    public void execute(GRunPrintReportAction action);

    public void execute(GRunOpenInExcelAction action);

    public void execute(GHideFormAction action);

    public void execute(GProcessFormChangesAction action);

    public Object execute(GRequestUserInputAction action);

    public void execute(GAsyncResultAction action);

    public void execute(GLogOutAction action);
}

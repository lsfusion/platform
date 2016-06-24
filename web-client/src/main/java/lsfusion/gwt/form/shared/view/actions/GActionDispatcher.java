package lsfusion.gwt.form.shared.view.actions;

public interface GActionDispatcher {
    public void execute(GFormAction action);

    public void execute(GReportAction action);

    public void execute(GRunOpenReportAction action);

    public Object execute(GChooseClassAction action);

    public void execute(GMessageAction action);

    public int execute(GConfirmAction action);

    public void execute(GLogMessageAction action);

    public void execute(GHideFormAction action);

    public void execute(GProcessFormChangesAction action);

    public Object execute(GRequestUserInputAction action);

    public void execute(GUpdateEditValueAction action);

    public void execute(GLogOutAction action);

    public void execute(GOpenUriAction action);

    public void execute(GEditNotPerformedAction action);

    public void execute(GAsyncGetRemoteChangesAction action);

    public void execute(GOpenFileAction action);

    public void execute(GExportFileAction action);
    
    public void execute(GFocusAction action);
}

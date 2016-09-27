package lsfusion.gwt.form.shared.view.actions;

public interface GActionDispatcher {
    void execute(GFormAction action);

    void execute(GReportAction action);

    void execute(GRunOpenReportAction action);

    Object execute(GChooseClassAction action);

    void execute(GMessageAction action);

    int execute(GConfirmAction action);

    void execute(GLogMessageAction action);

    void execute(GHideFormAction action);

    void execute(GProcessFormChangesAction action);

    Object execute(GRequestUserInputAction action);

    void execute(GUpdateEditValueAction action);

    void execute(GLogOutAction action);

    void execute(GOpenUriAction action);

    void execute(GEditNotPerformedAction action);

    void execute(GAsyncGetRemoteChangesAction action);

    void execute(GOpenFileAction action);

    void execute(GExportFileAction action);
    
    void execute(GFocusAction action);

    void execute(GActivateTabAction action);

    String execute(GLoadLinkAction action);
}

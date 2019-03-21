package lsfusion.gwt.shared.action;

public interface GActionDispatcher {
    void execute(GFormAction action);

    void execute(GReportAction action);

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

    String execute(GLoadLinkAction action);

    void execute(GBeepAction action);

    void execute(GActivateFormAction action);

    void execute(GMaximizeFormAction action);
}

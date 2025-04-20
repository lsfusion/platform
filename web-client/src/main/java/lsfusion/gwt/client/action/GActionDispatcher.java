package lsfusion.gwt.client.action;

import com.google.gwt.http.client.RequestException;

public interface GActionDispatcher {
    void execute(GFormAction action);

    void execute(GReportAction action);

    Object execute(GChooseClassAction action);

    void execute(GMessageAction action);

    Object execute(GConfirmAction action);

    void execute(GHideFormAction action);

    void execute(GDestroyFormAction action);

    void execute(GProcessFormChangesAction action);

    void execute(GProcessNavigatorChangesAction action);

    Object execute(GRequestUserInputAction action);

    void execute(GUpdateEditValueAction action);

    void execute(GLogOutAction action);

    void execute(GOpenUriAction action);

    void execute(GEditNotPerformedAction action);

    void execute(GAsyncGetRemoteChangesAction action);

    void execute(GOpenFileAction action);

    String execute(GReadAction action);

    void execute(GWriteAction action);

    String execute(GRunCommandAction action);

    String execute(GLoadLinkAction action);

    void execute(GBeepAction action);

    void execute(GActivateFormAction action);

    void execute(GMaximizeFormAction action);

    void execute(GCloseFormAction action);
    
    void execute(GChangeColorThemeAction action);

    void execute(GResetWindowsLayoutAction action);

    Object execute(GClientWebAction action);

    Object execute(GHttpClientAction action) throws RequestException;
    
    void execute(GOrderAction action);
    
    void execute(GFilterAction action);

    void execute(GFilterGroupAction action);
}

package lsfusion.interop.action;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.event.EventBus;

import java.io.IOException;
import java.util.Map;

public interface ClientActionDispatcher {

    void execute(FormClientAction action) throws IOException;

    Integer execute(ReportClientAction action);

    Object execute(ChooseClassClientAction action);

    void execute(UserChangedClientAction action);

    void execute(MessageClientAction action);

    int execute(ConfirmClientAction action);

    void execute(OpenFileClientAction action);

    void execute(OpenUriClientAction action);

    void execute(RunEditReportClientAction action);

    void execute(HideFormClientAction action);

    void execute(DestroyFormClientAction action);

    void execute(ProcessFormChangesClientAction action);

    void execute(ProcessNavigatorChangesClientAction action);

    Object execute(RequestUserInputClientAction action);

    void execute(EditNotPerformedClientAction action);

    void execute(UpdateEditValueClientAction action);

    void execute(AsyncGetRemoteChangesClientAction action);

    void execute(LogOutClientAction action);

    void execute(ExceptionClientAction action);

    String execute(LoadLinkClientAction action);

    void execute(CopyToClipboardClientAction action);

    Map<String, RawFileData> execute(UserLogsClientAction action);

    RawFileData execute(ThreadDumpClientAction action);

    void execute(BeepClientAction action);

    void execute(ActivateFormClientAction action);

    void execute(MaximizeFormClientAction action);

    void execute(CloseFormClientAction action);

    void execute(ChangeColorThemeClientAction action);

    void execute(ResetWindowsLayoutClientAction action);
    
    void execute(OrderClientAction action);
    
    void execute(FilterClientAction action);

    void execute(FilterGroupClientAction action);

    void execute(ClientWebAction action);

    Object execute(CopyReportResourcesClientAction action);

    EventBus getEventBus();

    void addCleanListener(ICleanListener daemonTask);
}

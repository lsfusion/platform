package lsfusion.interop.action;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.form.event.EventBus;

import java.io.IOException;
import java.util.Map;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    void execute(FormClientAction action);

    Integer execute(ReportClientAction action);

    Object execute(RuntimeClientAction action);

    void execute(ExportFileClientAction action) throws IOException;

    Object execute(ImportFileClientAction action);

    Object execute(MessageFileClientAction action);

    Object execute(ChooseClassClientAction action);

    void execute(UserChangedClientAction action);

    void execute(MessageClientAction action);

    int execute(ConfirmClientAction action);

    void execute(LogMessageClientAction action);

    void execute(OpenFileClientAction action);

    void execute(AudioClientAction action);

    void execute(RunEditReportClientAction action);

    void execute(HideFormClientAction action);

    void execute(ProcessFormChangesClientAction action);

    Object execute(RequestUserInputClientAction action);

    void execute(EditNotPerformedClientAction action);

    void execute(UpdateEditValueClientAction action);

    void execute(AsyncGetRemoteChangesClientAction action);

    void execute(LogOutClientAction action);

    void execute(ExceptionClientAction action);

    String execute(LoadLinkClientAction action);

    boolean execute(CopyToClipboardClientAction action);

    Map<String, RawFileData> execute(UserLogsClientAction action);

    RawFileData execute(ThreadDumpClientAction action);

    void execute(BeepClientAction action);

    void execute(ActivateFormClientAction action);

    void execute(MaximizeFormClientAction action);
    
    void execute(ChangeColorThemeClientAction action);
    
    void execute(ResetWindowsLayoutClientAction action);

    EventBus getEventBus();

    void addCleanListener(ICleanListener daemonTask);
}

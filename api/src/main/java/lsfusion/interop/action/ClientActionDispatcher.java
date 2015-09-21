package lsfusion.interop.action;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    void execute(FormClientAction action);

    Integer execute(ReportClientAction action);

    Object execute(RuntimeClientAction action);

    void execute(ExportFileClientAction action);

    Object execute(ImportFileClientAction action);

    Object execute(MessageFileClientAction action);

    Object execute(ChooseClassClientAction action);

    void execute(UserChangedClientAction action);

    void execute(MessageClientAction action);

    int execute(ConfirmClientAction action);

    void execute(LogMessageClientAction action);

    void execute(OpenFileClientAction action);

    void execute(SaveFileClientAction action);

    void execute(AudioClientAction action);

    void execute(RunPrintReportClientAction action);

    void execute(RunOpenInExcelClientAction action);

    void execute(RunEditReportClientAction action);

    void execute(HideFormClientAction action);

    void execute(ProcessFormChangesClientAction action);

    Object execute(RequestUserInputClientAction action);

    void execute(EditNotPerformedClientAction action);

    void execute(UpdateEditValueClientAction action);

    void execute(AsyncGetRemoteChangesClientAction action);

    void execute(LogOutClientAction action);
    
    void execute(FocusClientAction action);
}

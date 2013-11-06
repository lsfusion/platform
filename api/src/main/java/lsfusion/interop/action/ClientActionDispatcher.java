package lsfusion.interop.action;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    public void execute(FormClientAction action);

    public void execute(ReportClientAction action);

    public Object execute(RuntimeClientAction action);

    public void execute(ExportFileClientAction action);

    public Object execute(ImportFileClientAction action);

    public Object execute(MessageFileClientAction action);

    public Object execute(ChooseClassClientAction action);

    public void execute(UserChangedClientAction action);

    public void execute(MessageClientAction action);

    public int execute(ConfirmClientAction action);

    public void execute(LogMessageClientAction action);

    public void execute(OpenFileClientAction action);

    public void execute(AudioClientAction action);

    public void execute(RunPrintReportClientAction action);

    public void execute(RunOpenInExcelClientAction action);

    public void execute(RunEditReportClientAction action);

    public void execute(HideFormClientAction action);

    public void execute(ProcessFormChangesClientAction action);

    public Object execute(RequestUserInputClientAction action);

    public void execute(EditNotPerformedClientAction action);

    public void execute(UpdateEditValueClientAction action);

    public void execute(AsyncGetRemoteChangesClientAction action);

    public void execute(LogOutClientAction action);
}

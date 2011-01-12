package platform.interop.action;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    public void execute(FormClientAction action);

    public Object execute(RuntimeClientAction action);

    public void execute(ExportFileClientAction action);

    public Object execute(ImportFileClientAction action);

    public void execute(SleepClientAction action);

    public Object execute(MessageFileClientAction action);

    public void execute(UserChangedClientAction action);
    
    public void execute(UserReloginClientAction action);

    public void execute(MessageClientAction action);

    public Object execute(ResultClientAction action);

    public Object execute(CustomClientAction action);

    public void execute(ApplyClientAction action);

    public void execute(OpenFileClientAction action);
}

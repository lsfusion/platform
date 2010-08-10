package platform.interop.action;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    public Object execute(FormClientAction action);

    public Object execute(RuntimeClientAction action);

    public Object execute(ExportFileClientAction action);

    public Object execute(ImportFileClientAction action);

    public Object execute(SleepClientAction action);

    public Object execute(MessageFileClientAction action);

    public Object execute(UserChangedClientAction action);

    public Object execute(MessageClientAction action);

    public Object execute(ResultClientAction action);

    public Object execute(CustomClientAction action);
}

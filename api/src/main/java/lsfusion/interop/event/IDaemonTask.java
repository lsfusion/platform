package lsfusion.interop.event;

public interface IDaemonTask {

    public void start() throws Exception;

    public void stop();

    public void setEventBus(EventBus eventBus);
}

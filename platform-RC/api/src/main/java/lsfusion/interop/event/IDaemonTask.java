package lsfusion.interop.event;

public interface IDaemonTask {

    void start() throws Exception;

    void stop();

    void setEventBus(EventBus eventBus);
}

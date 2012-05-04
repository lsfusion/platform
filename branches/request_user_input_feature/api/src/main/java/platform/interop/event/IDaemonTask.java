package platform.interop.event;

public interface IDaemonTask extends Runnable {

    @Override
    public void run();

    public int getDelay();

    public int getPeriod();

    public void setEventBus(EventBus eventBus);
}

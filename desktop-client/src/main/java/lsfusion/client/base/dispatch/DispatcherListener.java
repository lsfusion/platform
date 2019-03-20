package lsfusion.client.base.dispatch;

public interface DispatcherListener {
    void dispatchingEnded();
    void dispatchingPostponedEnded(DispatcherInterface realDispatcher);
}

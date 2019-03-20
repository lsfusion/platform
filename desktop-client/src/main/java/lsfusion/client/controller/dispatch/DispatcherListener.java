package lsfusion.client.controller.dispatch;

public interface DispatcherListener {
    void dispatchingEnded();
    void dispatchingPostponedEnded(DispatcherInterface realDispatcher);
}

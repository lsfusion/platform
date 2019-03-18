package lsfusion.client.base.dispatch;

import lsfusion.client.base.dispatch.DispatcherInterface;

public interface DispatcherListener {
    void dispatchingEnded();
    void dispatchingPostponedEnded(DispatcherInterface realDispatcher);
}

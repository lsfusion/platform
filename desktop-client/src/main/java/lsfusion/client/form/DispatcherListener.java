package lsfusion.client.form;

import lsfusion.client.base.dispatch.DispatcherInterface;

public interface DispatcherListener {
    void dispatchingEnded();
    void dispatchingPostponedEnded(DispatcherInterface realDispatcher);
}

package lsfusion.client.form;

import lsfusion.client.form.dispatch.DispatcherInterface;

public interface DispatcherListener {
    void dispatchingEnded();
    void dispatchingPostponedEnded(DispatcherInterface realDispatcher);
}

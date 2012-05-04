package platform.server.lifecycle;

import platform.base.ArrayInstancer;
import platform.base.BaseUtils;
import platform.server.logics.BusinessLogics;

import static platform.server.lifecycle.LifecycleEvent.*;

public class LifecycleManager {
    private final Object listenersLock = new Object();

    private LifecycleListener listeners[] = new LifecycleListener[0];

    private final static ArrayInstancer<LifecycleListener> arrayInstancer = new BaseUtils.GenericTypeInstancer<LifecycleListener>(LifecycleListener.class);

    public LifecycleManager(LifecycleListener... listeners) {
        if (listeners != null && listeners.length > 0) {
            this.listeners = listeners;
        }
    }

    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (listenersLock) {
            listeners = BaseUtils.addElement(listeners, listener, arrayInstancer);
        }
    }

    public void addLifecycleListeners(LifecycleListener... newListeners) {
        synchronized (listenersLock) {
            listeners = BaseUtils.add(listeners, newListeners, arrayInstancer);
        }
    }

    public void fireLifecycleEvent(String type) {
        fireLifecycleEvent(type, null);
    }

    public void fireLifecycleEvent(String type, Object data) {
        if (listeners.length == 0) {
            return;
        }
        LifecycleEvent event = new LifecycleEvent(type, data);
        for (LifecycleListener listener : listeners) {
            listener.lifecycleEvent(event);
        }
    }

    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (listenersLock) {
            listeners = BaseUtils.removeElement(listeners, listener, arrayInstancer);
        }
    }

    public void fireStarting() {
        fireLifecycleEvent(STARTING);
    }

    public void fireStarted() {
        fireLifecycleEvent(STARTED);
    }

    public void firePong() {
        fireLifecycleEvent(PONG);
    }

    public void fireStopping() {
        fireLifecycleEvent(STOPPING);
    }

    public void fireStopped() {
        fireLifecycleEvent(STOPPED);
    }

    public void fireError(String error) {
        fireLifecycleEvent(ERROR, error);
    }

    public void fireMessage(String msg) {
        fireLifecycleEvent(MESSAGE, msg);
    }

    public void fireBlCreated(BusinessLogics bl) {
        fireLifecycleEvent(LOGICS_CREATED, bl);
    }
}

package lsfusion.server.lifecycle;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static lsfusion.server.lifecycle.LifecycleEvent.*;

public class LifecycleManager {
    private final Object listenersLock = new Object();

    private final Set<LifecycleListener> listeners = new TreeSet<>(LifecycleListener.ORDER_COMPARATOR);

    public LifecycleManager(LifecycleListener... listeners) {
        addLifecycleListeners(listeners);
    }

    public synchronized void addLifecycleListener(LifecycleListener listener) {
        listeners.add(listener);
    }

    public synchronized void addLifecycleListeners(LifecycleListener... newListeners) {
        Collections.addAll(listeners, newListeners);
    }

    public synchronized void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove(listener);
    }

    public synchronized void fireLifecycleEvent(String type) {
        if (listeners.size() == 0) {
            return;
        }
        LifecycleEvent event = new LifecycleEvent(type);
        for (LifecycleListener listener : listeners) {
            listener.lifecycleEvent(event);
        }
    }

    public void fireStarting() {
        fireLifecycleEvent(INIT);
    }

    public void fireStarted() {
        fireLifecycleEvent(STARTED);
    }

    public void fireStopping() {
        fireLifecycleEvent(STOPPING);
    }

    public void fireStopped() {
        fireLifecycleEvent(STOPPED);
    }

    public void fireError() {
        fireLifecycleEvent(ERROR);
    }
}

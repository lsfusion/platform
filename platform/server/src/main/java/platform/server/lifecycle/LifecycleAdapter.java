package platform.server.lifecycle;

import static platform.server.lifecycle.LifecycleEvent.*;

public class LifecycleAdapter implements LifecycleListener {
    private final int order;

    public LifecycleAdapter() {
        this(DAEMON_ORDER);
    }

    public LifecycleAdapter(int order) {
        this.order = order;
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        String type = event.getType();
        if (type.equals(INIT)) {
            onInit(event);
        } else if (type.equals(STARTED)) {
            onStarted(event);
        } else if (type.equals(STOPPING)) {
            onStopping(event);
        } else if (type.equals(STOPPED)) {
            onStopped(event);
        } else if (type.equals(ERROR)) {
            onError(event);
        } else {
            onOtherEvent(event);
        }
    }

    @Override
    public int getOrder() {
        return order;
    }

    protected void onInit(LifecycleEvent event) {
    }

    protected void onStarted(LifecycleEvent event) {
    }

    protected void onStopping(LifecycleEvent event) {
    }

    protected void onStopped(LifecycleEvent event) {
    }

    protected void onError(LifecycleEvent event) {
    }

    protected void onOtherEvent(LifecycleEvent event) {
    }
}

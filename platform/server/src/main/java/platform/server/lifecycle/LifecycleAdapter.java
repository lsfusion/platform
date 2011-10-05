package platform.server.lifecycle;

import platform.server.logics.BusinessLogics;

import static platform.server.lifecycle.LifecycleEvent.*;

public class LifecycleAdapter implements LifecycleListener {
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        String type = event.getType();
        if (type.equals(STARTING)) {
            onStarting(event);
        } else if (type.equals(STARTED)) {
            onStarted(event);
        } else if (type.equals(PONG)) {
            onPong(event);
        } else if (type.equals(STOPPING)) {
            onStopping(event);
        } else if (type.equals(STOPPED)) {
            onStopped(event);
        } else if (type.equals(ERROR)) {
            onError(event);
        } else if (type.equals(MESSAGE)) {
            onMessage(event);
        } else if (type.equals(LOGICS_CREATED)) {
            onLogicsCreated((BusinessLogics)event.getData());
        } else {
            onOtherEvent(event);
        }
    }

    protected void onLogicsCreated(BusinessLogics event) {
    }

    protected void onOtherEvent(LifecycleEvent event) {
    }

    protected void onMessage(LifecycleEvent event) {
    }

    protected void onError(LifecycleEvent event) {
    }

    protected void onStopped(LifecycleEvent event) {
    }

    protected void onStopping(LifecycleEvent event) {
    }

    protected void onPong(LifecycleEvent event) {
    }

    protected void onStarted(LifecycleEvent event) {
    }

    protected void onStarting(LifecycleEvent event) {
    }
}

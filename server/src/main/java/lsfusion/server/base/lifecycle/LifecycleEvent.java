package lsfusion.server.base.lifecycle;

import java.io.Serializable;

public final class LifecycleEvent implements Serializable {
    public final static String INIT = "INIT";
    public final static String STARTED = "STARTED";
    public final static String STOPPING = "STOPPING";
    public final static String STOPPED = "STOPPED";
    public final static String ERROR = "ERROR";

    private final String type;

    public LifecycleEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
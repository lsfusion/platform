package platform.server.lifecycle;

import java.io.Serializable;

public final class LifecycleEvent implements Serializable {
    public final static String INIT = "INIT";
    public final static String STARTED = "STARTED";
    public final static String STOPPING = "STOPPING";
    public final static String STOPPED = "STOPPED";
    public final static String ERROR = "ERROR";

    private final Object data;
    private final String type;

    public LifecycleEvent(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type + (data != null ? ":" + data : "");
    }
}
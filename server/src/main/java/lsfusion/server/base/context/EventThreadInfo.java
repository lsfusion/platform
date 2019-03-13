package lsfusion.server.base.context;

import lsfusion.server.base.lifecycle.EventServer;
import lsfusion.server.base.lifecycle.MonitorServer;
import lsfusion.server.remote.ContextAwarePendingRemoteObject;
import lsfusion.server.remote.RmiServer;

public class EventThreadInfo implements ThreadInfo {

    private final String eventName;

    public EventThreadInfo(String eventName) { // можно было бы кэшировать, но ради одного инстанцирования скорее всего не имеет смысла
        this.eventName = eventName;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    private static EventThreadInfo create(EventServer server, String type) {
        return new EventThreadInfo(type + ":" + server.getEventName());
    }    
    private static EventThreadInfo create(ContextAwarePendingRemoteObject object, String type) {
        return new EventThreadInfo(type + ":" + object.getSID());
    }    
    
    public static EventThreadInfo RMI(RmiServer server) {
        return create(server, "RMI");
    }
    public static EventThreadInfo RMI(ContextAwarePendingRemoteObject object) {
        return create(object, "RMI");
    }
    public static EventThreadInfo RMI(String eventName) {
        return new EventThreadInfo("RMI:" + eventName);
    }
    public static EventThreadInfo HTTP(MonitorServer server) {
        return create(server, "HTTP");
    }
    public static EventThreadInfo START() {
        return new EventThreadInfo("START");
    }
    public static EventThreadInfo TIMER(ContextAwarePendingRemoteObject object) {
        return create(object, "TIMER");
    }
    public static EventThreadInfo UNREFERENCED(ContextAwarePendingRemoteObject object) {
        return create(object, "UNREFERENCED");
    };

}

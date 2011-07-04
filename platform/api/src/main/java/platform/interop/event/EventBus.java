package platform.interop.event;

import java.io.Serializable;
import java.util.Map;
import java.util.WeakHashMap;

public class EventBus implements Serializable {

    private WeakHashMap<ValueEventListener, String> listeners = new WeakHashMap<ValueEventListener, String>();

    public void raiseEvent(ValueEvent event) {
        for (Map.Entry<ValueEventListener, String> entry : listeners.entrySet()) {
            if ((entry.getValue() != null) && (entry.getValue().equals(event.getSID()))) {
                entry.getKey().actionPerfomed(event);
            }
        }
    }

    public void addListener(ValueEventListener listener, String eventSID) {
        listeners.put(listener, eventSID);
    }

    public void enterValue(Object value, String SID) {
        raiseEvent(new ValueEvent(SID, value));
    }
}

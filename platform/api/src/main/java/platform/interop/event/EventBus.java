package platform.interop.event;

import platform.base.WeakLinkedHashSet;

import java.io.Serializable;
import java.util.*;

public class EventBus implements Serializable {

    private WeakHashMap<ValueEventListener, String> listeners = new WeakHashMap<ValueEventListener, String>();

    public void raiseEvent(ValueEvent event) {
        for (Map.Entry<ValueEventListener, String> entry : listeners.entrySet()) {
            if ((entry.getValue() != null) && (entry.getValue().equals(event.getSID()))) {
                entry.getKey().actionPerfomed(event);
            }
        }
    }

    public void addListener(ValueEventListener listener) {
        listeners.put(listener, listener.getEventSID());
    }

    public void enterValue(int value, String SID) {
        raiseEvent(new ValueEvent(SID, value));
    }
}

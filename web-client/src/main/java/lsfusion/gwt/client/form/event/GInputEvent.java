package lsfusion.gwt.client.form.event;

import com.google.gwt.user.client.Event;

import java.io.Serializable;
import java.util.Map;

public abstract class GInputEvent implements Serializable {
    public GInputEvent() {
    }

    public abstract boolean isEvent(Event event);
}

package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;

public class EventHandler {

    public final Event event;

    public EventHandler(Event event) {
        this.event = event;
    }

    public boolean consumed;

    public void consume() {
        consume(false);
    }

    // called when we find out that event should be proceeded by native component
    // so we want to finish consuming events (for example LEFT, RIGHT in grid), but we want to ve proceeded by native component (for example text input in property editor)
    public void consume(boolean propagateToNative) {
        if(!propagateToNative)
            GwtClientUtils.stopPropagation(event);
        consumed = true;
    }
}

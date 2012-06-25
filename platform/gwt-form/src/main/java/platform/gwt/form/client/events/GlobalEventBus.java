package platform.gwt.form.client.events;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.google.web.bindery.event.shared.SimpleEventBus;

public class GlobalEventBus {
    private static SimpleEventBus eventBus = new SimpleEventBus();

    public static <H> HandlerRegistration addHandler(Event.Type<H> type, H handler) {
        return eventBus.addHandler(type, handler);
    }

    public static <H> void fireEvent(Event<H> event) {
        eventBus.fireEvent(event);
    }
}

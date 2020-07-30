package lsfusion.gwt.client.form.event;

import com.google.gwt.user.client.Event;

import java.util.Map;

public class GKeyInputEvent extends GInputEvent {

    public GKeyStroke keyStroke;

    public GKeyInputEvent() {
    }

    public GKeyInputEvent(GKeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GKeyInputEvent && keyStroke.equals(((GKeyInputEvent) o).keyStroke);
    }

    @Override
    public int hashCode() {
        return keyStroke.hashCode();
    }

    @Override
    public String toString() {
        return keyStroke.toString();
    }

    @Override
    public boolean isEvent(Event event) {
        return keyStroke.isEvent(event);
    }
}

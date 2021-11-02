package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

public interface CellEditor {

    void start(Event editEvent, Element parent, Object oldValue);

    default boolean stopProcessingEvent(Event event) {
        return false;
    }
}

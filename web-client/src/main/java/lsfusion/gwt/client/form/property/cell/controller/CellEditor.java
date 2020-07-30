package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;

public interface CellEditor {

    default void onBrowserEvent(Element parent, EventHandler handler) {
    }

    default void commitEditing(Element parent) { // force commit (on binding)
    }

    void startEditing(Event editEvent, Element parent, Object oldValue);
}

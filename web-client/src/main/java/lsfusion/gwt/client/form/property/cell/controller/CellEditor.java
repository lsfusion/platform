package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;

public interface CellEditor {

    void start(Event editEvent, Element parent, Object oldValue);
}

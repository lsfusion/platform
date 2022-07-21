package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;

public interface CellEditor {

    void start(EventHandler handler, Element parent, Object oldValue);

    default Object getDefaultNullValue() { return null; };
}

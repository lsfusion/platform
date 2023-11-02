package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.property.PValue;

public interface CellEditor {

    void start(EventHandler handler, Element parent, ResizableComplexPanel attachContainer, PValue oldValue);

    default PValue getDefaultNullValue() { return null; };
}

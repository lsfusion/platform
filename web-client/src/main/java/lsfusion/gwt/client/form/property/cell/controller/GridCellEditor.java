package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public interface GridCellEditor {

    void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext);

    default void onBrowserEvent(Element parent, Event event, Runnable consumed) {
    }

    void startEditing(Event editEvent, Element parent, Object oldValue);

    boolean replaceCellRenderer();
}

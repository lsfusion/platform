package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class CustomCellEditor implements CellEditor {

    private final EditManager editManager;
    private final GPropertyDraw property;
    private final String startEditingFunction;
    private final String commitEditingFunction;
    private final String onBrowserEventFunction;

    public CustomCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunctions) {
        this.editManager = editManager;
        this.property = property;

        String[] functions = customEditorFunctions.split(":");
        this.startEditingFunction = functions[0];
        this.commitEditingFunction = functions[1];
        this.onBrowserEventFunction = functions[2];
    }

    @Override
    public void commitEditing(Element parent) {
        commit(parent);
    }

    protected native void commit(Element element)/*-{
        $wnd[this.@CustomCellEditor::commitEditingFunction](element);
    }-*/;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        startEditing(parent);
    }

    protected native void startEditing(Element element)/*-{
        $wnd[this.@CustomCellEditor::startEditingFunction](element);
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if (onBrowserEventFunction != null)
            onBrowserEvent();
    }

    protected native void onBrowserEvent()/*-{
        $wnd[this.@CustomCellEditor::onBrowserEventFunction]();
    }-*/;
}

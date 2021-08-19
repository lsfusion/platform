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
    private final String customEditorFunction;

    public CustomCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunction) {
        this.editManager = editManager;
        this.property = property;
        this.customEditorFunction = customEditorFunction;
    }

    @Override
    public void commitEditing(Element parent) {
        commit(parent);
    }

    protected native void commit(Element element)/*-{
        $wnd[this.@CustomCellEditor::customEditorFunction]().commit(element);
    }-*/;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        startEditing(parent);
    }

    protected native void startEditing(Element element)/*-{
        $wnd[this.@CustomCellEditor::customEditorFunction]().startEditing(element);
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        onBrowserEvent();
    }

    protected native void onBrowserEvent()/*-{
        var customEditorFunction = $wnd[this.@CustomCellEditor::customEditorFunction]();
        if (customEditorFunction.onBrowserEvent !== 'undefined')
            customEditorFunction.onBrowserEvent();
    }-*/;
}

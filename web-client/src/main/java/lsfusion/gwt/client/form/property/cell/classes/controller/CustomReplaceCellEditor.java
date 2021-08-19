package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.ReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class CustomReplaceCellEditor implements ReplaceCellEditor {

    private final EditManager editManager;
    private final GPropertyDraw property;
    private final String customEditorFunction;

    public CustomReplaceCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunction) {
        this.editManager = editManager;
        this.property = property;
        this.customEditorFunction = customEditorFunction;
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize) {
        render(cellParent);
    }

    protected native void render(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::customEditorFunction]().render(element);
    }-*/;

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        clearRender(cellParent);
        GwtClientUtils.removeAllChildren(cellParent);
    }

    protected native void clearRender(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::customEditorFunction].clearRender(element);
    }-*/;

    @Override
    public void commitEditing(Element parent) {
        commit(parent);
    }

    protected native void commit(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::customEditorFunction].commit(element);
    }-*/;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        startEditing(parent);
    }

    protected native void startEditing(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::customEditorFunction].startEditing(element);
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        onBrowserEvent();
    }

    protected native void onBrowserEvent()/*-{
        var customEditorFunction = $wnd[this.@CustomReplaceCellEditor::customEditorFunction]();
        if (customEditorFunction.onBrowserEvent !== 'undefined')
            customEditorFunction.onBrowserEvent();
    }-*/;
}

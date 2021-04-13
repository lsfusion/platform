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

    private final String startEditingFunction;
    private final String commitEditingFunction;
    private final String renderFunction;
    private final String clearRenderFunction;
    private final String onBrowserEventFunction;

    public CustomReplaceCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunctions) {
        this.editManager = editManager;
        this.property = property;

        String[] functions = customEditorFunctions.split(":");
        this.renderFunction = functions[0];
        this.startEditingFunction = functions[1];
        this.commitEditingFunction = functions[2];
        this.clearRenderFunction = functions[3];
        this.onBrowserEventFunction = functions[4];
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize) {
        render(cellParent);
    }

    protected native void render(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::renderFunction](element);
    }-*/;

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        clearRender(cellParent);
        GwtClientUtils.removeAllChildren(cellParent);
    }

    protected native void clearRender(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::clearRenderFunction](element);
    }-*/;

    @Override
    public void commitEditing(Element parent) {
        commit(parent);
    }

    protected native void commit(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::commitEditingFunction](element);
    }-*/;

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        startEditing(parent);
    }

    protected native void startEditing(Element element)/*-{
        $wnd[this.@CustomReplaceCellEditor::startEditingFunction](element);
    }-*/;

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        if (onBrowserEventFunction != null)
            onBrowserEvent();
    }

    protected native void onBrowserEvent()/*-{
        $wnd[this.@CustomReplaceCellEditor::onBrowserEventFunction]();
    }-*/;
}

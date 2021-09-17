package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class CustomReplaceCellEditor extends RequestReplaceValueCellEditor implements CustomCellEditor {

    private final GPropertyDraw property;

    private final String renderFunction;
    private final JavaScriptObject customEditor;

    @Override
    public String getRenderFunction() {
        return renderFunction;
    }

    @Override
    public JavaScriptObject getCustomEditor() {
        return customEditor;
    }

    public CustomReplaceCellEditor(EditManager editManager, GPropertyDraw property, String renderFunction, JavaScriptObject customEditor) {
        super(editManager);
        this.property = property;

        this.renderFunction = renderFunction;
        this.customEditor = customEditor;
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {
        CustomCellEditor.super.render(cellParent, renderContext, renderedSize, oldValue);
    }

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        // we'll assume that everything is done in render method
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        super.clearRender(cellParent, renderContext);

        CustomCellEditor.super.clearRender(cellParent, renderContext);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        super.onBrowserEvent(parent, handler);

        CustomCellEditor.super.onBrowserEvent(parent, handler);
    }

    // actually should be in CustomCellRenderer but it's an interface and in GWT it's not possible

    // FACTORY

    public static CellEditor create(EditManager editManager, GPropertyDraw property, String customEditorFunction) {
        JavaScriptObject customEditor = getCustomEditor(customEditorFunction);

        String functionName = "Input";
        if(hasRenderFunction(functionName, customEditor))
            return new CustomTextCellEditor(editManager, property, functionName, customEditor);

        functionName = "Window";
        if(hasRenderFunction(functionName, customEditor))
            return new CustomWindowCellEditor(editManager, property, functionName, customEditor);

        return new CustomReplaceCellEditor(editManager, property, "", customEditor);
    }

    private static native JavaScriptObject getCustomEditor(String customEditorFunction)/*-{
        return $wnd[customEditorFunction]();
    }-*/;

    // COMMON METHODS

    private static native boolean hasRenderFunction(String functionName, JavaScriptObject customEditor)/*-{
        return customEditor['render' + functionName] !== undefined;
    }-*/;

    public static native void render(String functionName, JavaScriptObject customEditor, Element element, JavaScriptObject controller)/*-{
        customEditor['render' + functionName](element, controller);
    }-*/;

    private void forceCommit(Element parent) {
        commit(parent, CommitReason.FORCED);
    }

    public static native JavaScriptObject getController(CellEditor thisObj, Element cellParent)/*-{
        return {
            setDeferredCommitOnBlur: function (deferredCommitOnBlur) {
                thisObj.@CustomTextCellEditor::setDeferredCommitOnBlur(*)(deferredCommitOnBlur);
            },
            commit: function (value) {
                if(arguments.length === 1)
                    thisObj.@lsfusion.gwt.client.form.property.cell.classes.controller.ARequestValueCellEditor::commitValue(*)(cellParent, value);
                else
                    thisObj.@CustomReplaceCellEditor::forceCommit(*)(cellParent);
            },
            cancel: function () {
                thisObj.@RequestCellEditor::cancel(*)(cellParent);
            }
        }
    }-*/;

    public static native boolean hasGetValue(JavaScriptObject customEditor)/*-{
        return customEditor.getValue !== undefined;
    }-*/;

    public static native Object getValue(JavaScriptObject customEditor, Element element)/*-{
        return customEditor.getValue(element);
    }-*/;

    public static native void clear(JavaScriptObject customEditor, Element element)/*-{
        if (customEditor.clear !== undefined)
            customEditor.clear(element);
    }-*/;

    public static native void onBeforeFinish(JavaScriptObject customEditor, Element element)/*-{
        if (customEditor.onBeforeFinish !== undefined)
            customEditor.onBeforeFinish(element);
    }-*/;

    public static native void onBrowserEvent(JavaScriptObject customEditor, Event event, Element element)/*-{
        if (customEditor.onBrowserEvent !== undefined)
            customEditor.onBrowserEvent(event, element);
    }-*/;
}

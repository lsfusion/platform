package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class CustomReplaceCellEditor extends RequestReplaceValueCellEditor implements CustomCellEditor {

    private final GPropertyDraw property;

    private final String renderFunction;
    private final GType type;
    private final JavaScriptObject customEditor;

    @Override
    public String getRenderFunction() {
        return renderFunction;
    }

    @Override
    public GType getType() {
        return type;
    }

    @Override
    public JavaScriptObject getCustomEditor() {
        return customEditor;
    }

    public CustomReplaceCellEditor(EditManager editManager, GPropertyDraw property, GType type, String renderFunction, JavaScriptObject customEditor) {
        super(editManager);
        this.property = property;

        this.renderFunction = renderFunction;
        this.type = type;
        this.customEditor = customEditor;
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {
        CustomCellEditor.super.render(cellParent, renderContext, oldValue, renderedWidth, renderedHeight);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, PValue oldValue) {
        // we'll assume that everything is done in render method
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        super.clearRender(cellParent, renderContext, cancel);

        CustomCellEditor.super.clearRender(cellParent, renderContext, cancel);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        super.onBrowserEvent(parent, handler);

        CustomCellEditor.super.onBrowserEvent(parent, handler);
    }

    // actually should be in CustomCellRenderer but it's an interface and in GWT it's not possible

    // FACTORY

    public static CustomCellEditor create(EditManager editManager, GPropertyDraw property, GType type, String customEditorFunction) {
        JavaScriptObject customEditor = getCustomFunction(customEditorFunction);

        String functionName = "Input";
        if(hasRenderFunction(functionName, customEditor))
            return new CustomTextCellEditor(editManager, property, type, functionName, customEditor);

        functionName = "Dialog";
        if(hasRenderFunction(functionName, customEditor))
            return new CustomWindowCellEditor(editManager, property, type, functionName, customEditor);

        return new CustomReplaceCellEditor(editManager, property, type, "", customEditor);
    }

    public static JavaScriptObject getCustomFunction(String customEditorFunction) {
        return GwtClientUtils.call(GwtClientUtils.getGlobalField(customEditorFunction));
    }

    // COMMON METHODS

    private static native boolean hasRenderFunction(String functionName, JavaScriptObject customEditor)/*-{
        return customEditor['render' + functionName] !== undefined;
    }-*/;

    public static native void render(String functionName, JavaScriptObject customEditor, Element element, JavaScriptObject controller, JavaScriptObject value)/*-{
        customEditor['render' + functionName](element, controller, value);
    }-*/;

    private static void forceCommit(CustomCellEditor cellEditor, Element parent) {
        cellEditor.commit(parent, CommitReason.FORCED);
    }

    private static void commitJSValue(CustomCellEditor cellEditor, Element parent, JavaScriptObject value) {
        cellEditor.commitValue(parent, GSimpleStateTableView.convertFromJSValue(cellEditor.getType(), value));
    }

    private static void setDeferredCommit(CustomCellEditor cellEditor, boolean deferredCommitOnBlur) {
        cellEditor.setDeferredCommitOnBlur(deferredCommitOnBlur);
    }

    public static native JavaScriptObject getController(CustomCellEditor thisObj, Element cellParent)/*-{
        return {
            setDeferredCommitOnBlur: function (deferredCommitOnBlur) {
                @CustomReplaceCellEditor::setDeferredCommit(*)(thisObj, deferredCommitOnBlur);
            },
            commit: function (value) {
                if(arguments.length === 1)
                    @lsfusion.gwt.client.form.property.cell.classes.controller.CustomReplaceCellEditor::commitJSValue(*)(thisObj, cellParent, value);
                else
                    @CustomReplaceCellEditor::forceCommit(*)(thisObj, cellParent);
            },
            cancel: function () {
                thisObj.@CustomReplaceCellEditor::cancel(Lcom/google/gwt/dom/client/Element;)(cellParent);
            },
            getColorThemeName: function () {
                return @lsfusion.gwt.client.view.MainFrame::colorTheme.@java.lang.Enum::name()();
            }
        }
    }-*/;

    public static native boolean hasGetValue(JavaScriptObject customEditor)/*-{
        return customEditor.getValue !== undefined;
    }-*/;

    public static native JavaScriptObject getValue(JavaScriptObject customEditor, Element element)/*-{
        return customEditor.getValue(element);
    }-*/;

    public static native void clear(JavaScriptObject customEditor, Element element, boolean cancel)/*-{
        if (customEditor.clear !== undefined)
            customEditor.clear(element, cancel);
    }-*/;

    public static native void onBrowserEvent(JavaScriptObject customEditor, Event event, Element element)/*-{
        if (customEditor.onBrowserEvent !== undefined)
            customEditor.onBrowserEvent(event, element);
    }-*/;
}

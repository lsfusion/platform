package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class CustomTextCellEditor extends SimpleTextBasedCellEditor implements CustomCellEditor {

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

    public CustomTextCellEditor(EditManager editManager, GPropertyDraw property, GType type, String renderFunction, JavaScriptObject customEditor) {
        super(editManager, property);

        this.renderFunction = renderFunction;
        this.type = type;
        this.customEditor = customEditor;
    }

    // we're working with input element here
    public InputElement getCustomElement(Element parent) {
        return getInputElement(parent);
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {
        super.render(cellParent, renderContext, renderedSize, oldValue);

        CustomCellEditor.super.render(cellParent, renderContext, renderedSize, oldValue);
    }

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        if(CustomReplaceCellEditor.hasGetValue(customEditor))
            return CustomCellEditor.super.getValue(parent, contextAction);

        return super.getValue(parent, contextAction);
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
}

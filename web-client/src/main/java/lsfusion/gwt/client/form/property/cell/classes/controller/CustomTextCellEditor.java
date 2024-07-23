package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class CustomTextCellEditor extends TextBasedCellEditor implements CustomCellEditor {

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
        return inputElement;
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) throws InvalidEditException {
        if(CustomReplaceCellEditor.hasGetValue(customEditor))
            return CustomCellEditor.super.getCommitValue(parent, contextAction);

        return super.getCommitValue(parent, contextAction);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        super.start(handler, parent, renderContext, notFocusable, oldValue);

        CustomCellEditor.super.render(parent, renderContext, oldValue, null, null);
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        CustomCellEditor.super.clearRender(parent, null, cancel);

        super.stop(parent, cancel, blurred);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        super.onBrowserEvent(parent, handler);

        CustomCellEditor.super.onBrowserEvent(parent, handler);
    }
}

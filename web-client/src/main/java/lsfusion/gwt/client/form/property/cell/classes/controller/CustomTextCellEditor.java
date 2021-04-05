package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public class CustomTextCellEditor extends TextBasedCellEditor {

    private final String renderFunction;
    private final String clearRenderFunction;
    private boolean deferredCommitOnBlur = true;

    public CustomTextCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunctions) {
        super(editManager, property);

        String[] functions = customEditorFunctions.split(":");
        renderFunction = functions[0];
        clearRenderFunction = functions[1];
    }

    @Override
    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        return inputText == null || inputText.isEmpty() ? null : inputText;
    }

    @Override
    protected Element setupInputElement(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize) {
        Element input = super.setupInputElement(cellParent, renderContext, renderedSize);
        render(input, getEditor());

        return input;
    }

    public void setDeferredCommitOnBlur(boolean deferredCommitOnBlur) {
        this.deferredCommitOnBlur = deferredCommitOnBlur;
    }

    protected native JavaScriptObject getEditor()/*-{
        var thisObj = this;
        return {
            setDeferredCommitOnBlur: function (deferredCommitOnBlur) {
                return thisObj.@CustomTextCellEditor::setDeferredCommitOnBlur(*)(deferredCommitOnBlur);
            }
        }
    }-*/;

    protected native void render(Element element, JavaScriptObject editor)/*-{
        $wnd[this.@CustomTextCellEditor::renderFunction](element, editor);
    }-*/;

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        clearRender(cellParent);
        GwtClientUtils.removeAllChildren(cellParent);
    }

    protected native void clearRender(Element element)/*-{
        $wnd[this.@CustomTextCellEditor::clearRenderFunction](element);
    }-*/;

    @Override
    public void validateAndCommit(Element parent, boolean cancelIfInvalid, boolean blurred) {
        //some libraries set values after the blur. to solve this there is a SmartScheduler that sets the values in the field before the blur
        if (deferredCommitOnBlur) {
            SmartScheduler.getInstance().scheduleDeferred(() -> super.validateAndCommit(parent, cancelIfInvalid, blurred));
        } else {
            super.validateAndCommit(parent, cancelIfInvalid, blurred);
        }
    }
}

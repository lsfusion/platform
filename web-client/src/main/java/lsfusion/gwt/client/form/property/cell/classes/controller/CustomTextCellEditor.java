package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public class CustomTextCellEditor extends TextBasedCellEditor {

    private final String renderFunction;
    private final String clearRenderFunction;
    private boolean deferredCommitOnBlur = false;

    public CustomTextCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunctions) {
        super(editManager, property);

        String[] split = customEditorFunctions.split(":");
        renderFunction = split[0];
        clearRenderFunction = split[1];
    }

    @Override
    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        return inputText == null || inputText.isEmpty() ? null : inputText;
    }

    @Override
    protected Element setupInputElement(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize) {
        Element input = super.setupInputElement(cellParent, renderContext, renderedSize);
        render(input, getController());

        return input;
    }

    public void setDeferredCommitOnBlur(boolean deferredCommitOnBlur) {
        this.deferredCommitOnBlur = deferredCommitOnBlur;
    }

    protected native JavaScriptObject getController()/*-{
        var thisObj = this;
        return {
            setDeferredCommitOnBlur: function (deferredCommitOnBlur) {
                return thisObj.@CustomTextCellEditor::setDeferredCommitOnBlur(*)(deferredCommitOnBlur);
            }
        }
    }-*/;

    protected native void render(Element element, JavaScriptObject controller)/*-{
        $wnd[this.@CustomTextCellEditor::renderFunction](element, controller);
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
        //some libraries set values after the blur. to solve this there is a scheduleDeferred that sets the values in the field before the event
        if (deferredCommitOnBlur)
            Scheduler.get().scheduleDeferred(() -> super.validateAndCommit(parent, cancelIfInvalid, blurred)); //scheduler because autocomplete works with minimal timeout
        else
            super.validateAndCommit(parent, cancelIfInvalid, blurred);
    }
}

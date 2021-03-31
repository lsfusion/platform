package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public class CustomTextCellEditor extends TextBasedCellEditor {

    private final String renderFunction;
    private final String clearRenderFunction;

    public CustomTextCellEditor(EditManager editManager, GPropertyDraw property, String customEditorFunctions) {
        super(editManager, property);

        String[] split = customEditorFunctions.split(":");
        renderFunction = split[0];
        clearRenderFunction = split[1];
    }

    @Override
    protected Object tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        return inputText.isEmpty() ? null : inputText;
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize) {
        render(cellParent);
    }

    protected native void render(Element element)/*-{
        $wnd[this.@CustomTextCellEditor::renderFunction](element);
    }-*/;

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext) {
        clearRender(cellParent);
    }

    protected native void clearRender(Element element)/*-{
        $wnd[this.@CustomTextCellEditor::clearRenderFunction](element);
    }-*/;

    @Override
    public void validateAndCommit(Element parent, boolean cancelIfInvalid, boolean blurred) {
        Scheduler.get().scheduleDeferred(() -> super.validateAndCommit(parent, cancelIfInvalid, blurred)); //scheduler because autocomplete works with minimal timeout
    }
}

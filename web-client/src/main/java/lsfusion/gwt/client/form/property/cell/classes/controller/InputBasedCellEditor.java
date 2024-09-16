package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GFullInputType;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.view.TextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

// now it's a sort of mix of RequestKeepValueCellEditor and RequestReplaceValueCellEditor (depending on needReplace)
public abstract class InputBasedCellEditor extends RequestReplaceValueCellEditor {

    protected final GPropertyDraw property;

    public InputBasedCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager);

        this.property = property;
    }

    protected InputElement inputElement;
    protected GInputType inputElementType;
    protected String oldValue;

    public static boolean isInputKeyEvent(Event event, boolean isMultiLine) {
        return GKeyStroke.isInputKeyEvent(event, true, isMultiLine);
    }

    protected boolean needReplace(Element cellParent) {
        return getRenderInputElement(cellParent) == null;
    }
    private InputElement getRenderInputElement(Element cellParent) {
        return InputBasedCellRenderer.getInputElement(cellParent);
    }

    @Override
    public boolean needReplace(Element cellParent, RenderContext renderContext) {
        return needReplace(cellParent);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {

        //we need this order (focus before setValue) for single click editing IntegralCellEditor (type=number)
        // and it's better to do that before input.click, because for input with type color, if not focused it is shown in the upper left corner
        boolean needReplace = needReplace(parent);
        if(needReplace)
            FocusUtils.focus(inputElement, FocusUtils.Reason.REPLACE);
        else { // not replaced
            inputElement = getRenderInputElement(parent);
            inputElementType = InputBasedCellRenderer.getInputElementType(inputElement);

            //getInputValue() must be before onInputReady as it affects daterangepicker behaviour. onInputReady trigger the creation dateTimePicker, if the date was null
            //then getInputElementValue saves today's date instead of null, and when you press clear, it calls stop, which takes the oldValue
            // and returns it back and we get today's date instead of null
            this.oldValue = getInputValue();

            if(notFocusable) // binding or mouse change on not focusable property
                FocusUtils.focus(inputElement, FocusUtils.Reason.NOTFOCUSABLE);
        }

        CellRenderer.setIsEditing(parent, inputElement, true);

        if(!needReplace) {
            parent.addClassName("property-hide-toolbar");

            handler.consume(true, false);
        }
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if (!needReplace(parent)) {
            parent.removeClassName("property-hide-toolbar");

            setInputValue(parent, oldValue);
        }

        CellRenderer.setIsEditing(parent, inputElement, false);

        inputElement = null;
        inputElementType = null;
    }

    protected void setInputValue(Element parent, String value) {
        setInputValue(inputElement, value);
    }
    public static void setInputValue(InputElement element, String value) {
        element.setValue(value);
    }
    private String getInputValue() {
        return inputElement.getValue();
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {
        assert needReplace(cellParent);

        RendererType rendererType = renderContext.getRendererType();
        GFullInputType editFullInputType = InputBasedCellRenderer.getInputType(property, rendererType);
        inputElement = InputBasedCellRenderer.createInputElement(property, editFullInputType);

        GInputType editInputType = editFullInputType.inputType;
        inputElementType = editInputType;
        InputBasedCellRenderer.appendInputElement(cellParent, inputElement, true, false, editInputType);
        if(editInputType.isStretchText())
            CellRenderer.renderTextAlignment(inputElement, property.getHorzTextAlignment(), property.getVertTextAlignment());
        GFormController.setFont(inputElement, GFormController.getFont(property, renderContext));

        // input doesn't respect justify-content, stretch, plus we want to include paddings in input (to avoid having "selection border")
        // we have to set sizes that were rendered, since input elements have really unpredicatble content sizes
        if(renderedWidth != null)
            inputElement.getStyle().setWidth(renderedWidth, Style.Unit.PX);
        if(renderedHeight != null)
            inputElement.getStyle().setHeight(renderedHeight, Style.Unit.PX);
    }

    @Override
    public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        assert needReplace(cellParent);

        TextBasedCellRenderer.clearTextPadding(cellParent);

//        TextBasedCellRenderer.clearBasedTextFonts(property, element.getStyle(), renderContext);

//        TextBasedCellRenderer.clearRender(property, element.getStyle(), renderContext);

        clearInputElement(cellParent);

        super.clearRender(cellParent, renderContext, cancel);
    }

    public static void clearInputElement(Element cellParent) {
        if(!GwtClientUtils.isTDorTH(cellParent))
            GwtClientUtils.clearFlexParentElement(cellParent);
    }
}

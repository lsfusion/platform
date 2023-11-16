package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.SimpleTextBasedCellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;

// the renderer which may be renderered as plain input (or td in table)
public abstract class SimpleTextBasedCellRenderer extends InputBasedCellRenderer {

    public SimpleTextBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    public static void render(GPropertyDraw property, Element element, RenderContext renderContext, boolean multiLine) {
        CellRenderer.setBasedTextFonts(property, element, renderContext);

        element.addClassName("text-based-prop-value");
        if(multiLine)
            element.addClassName("text-based-prop-wrap");
    }

    public static void clearRender(GPropertyDraw property, Element element, RenderContext renderContext, boolean multiLine) {
        element.removeClassName("text-based-prop-value");
        if(multiLine)
            element.removeClassName("text-based-prop-wrap");
    }

    public static void setPadding(Element element) {
        element.addClassName("text-based-prop-sized");
    }

    public static void clearPadding(Element element) {
        element.removeClassName("text-based-prop-sized");
    }

    @Override
    public boolean canBeRenderedInTD() {
        if(isTagInput()) // input / textareas has fixed sizes, so can be used with multiline fixed sizes
            return true;

        // td always respects the inner text height, so if it is multi line and not autosized, we have wrap the content into a div
        if (isMultiLine() && property.valueHeight != -1)
            return false;

        // input we have to render in td, since input is a void element, and it can not have children (and they are needed for the toolbar)
        // so the hack is to render it
        return getTag() == null;
    }

    @Override
    public void renderPanelLabel(Widget label) {
        // we're not setting form-label since it's mostly used only for layouting, which we do ourselves
//        if(property.panelCaptionVertical)
//            label.addStyleName("form-label");
//        else
//            label.addStyleName("col-form-label");
    }

    @Override
    protected InputElement createInput(GPropertyDraw property, RendererType rendererType) {
        return createInputElement(property, rendererType);
    }

    public static InputElement createInputElement(GPropertyDraw property, RendererType rendererType) {
        return property.createTextInputElement(rendererType);
    }

    public static InputElement getFocusEventTarget(Element parent, Event event) {
        InputElement inputElement = getSimpleInputElement(parent);
        return inputElement == event.getEventTarget().cast() ? inputElement : null;
    }

    public static boolean isMultiLineInput(Element parent) {
        return TextAreaElement.is(getSimpleInputElement(parent));
    }

    public static Element getSizeElement(Element element) {
        InputElement sizeElement;
        if(isToolbarContainer(element) && (sizeElement = getSimpleInputElement(element)) != null)
            return sizeElement;
        return element;
    }

    private final static String inputElementProp = "inputElement";
    public static InputElement getSimpleInputElement(Element element) {
        return (InputElement) element.getPropertyObject(inputElementProp);
    }
    public static void setSimpleInputElement(Element element, InputElement inputElement) {
        element.setPropertyObject(inputElementProp, inputElement);
        setSimpleReadonlyFnc(element, inputElement);
        setFocusElement(element, inputElement);
    }
    public static void clearSimpleInputElement(Element element) {
        element.setPropertyObject(inputElementProp, null);
        clearSimpleReadonlyFnc(element);
        clearFocusElement(element);
    }
    public static void setSimpleReadonlyFnc(Element element, InputElement inputElement) {
        setReadonlyFnc(element, getSimpleReadonlyFnc(inputElement));
    }
    public static void clearSimpleReadonlyFnc(Element element) {
        clearReadonlyFnc(element);
    }
    private static native JavaScriptObject getSimpleReadonlyFnc(InputElement element)/*-{
        return function(readonly) {
            $wnd.setDisabledNative(element, readonly != null && readonly);
            $wnd.setReadonlyNative(element, readonly != null && !readonly);
        }
    }-*/;

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {

        boolean renderedAlignment = false;
        InputElement inputElement = null;

        boolean isTDOrTH = GwtClientUtils.isTDorTH(element); // because canBeRenderedInTD can be true
        boolean isInput = isTagInput();
        boolean multiLine = isMultiLine();

        if(isInput && (isTDOrTH || isToolbarContainer(element))) {
            // assert isTDOrTH != isToolbarContainer(element);
            inputElement = SimpleTextBasedCellEditor.renderInputElement(element, property, multiLine, renderContext, isTDOrTH);
            renderedAlignment = true;
        } else {
            // otherwise we'll use flex alignment (however text alignment would also do)
            // there is some difference in div between align-items center and vertical align baseline / middle, and align items center seems to be more accurate (and better match input vertical align baseline / middle)
            if(isTDOrTH || isInput) {
                renderTextAlignment(property, element, isInput, renderContext.getRendererType());
                renderedAlignment = true;
            }
            SimpleTextBasedCellRenderer.render(property, element, renderContext, multiLine);

            if(isInput)
                inputElement = (InputElement) element;
        }

        if(inputElement != null)
            setSimpleInputElement(element, inputElement);

        setPadding(getSizeElement(element));

        if(property.isEditableNotNull())
            element.addClassName("text-based-value-required");

        return renderedAlignment;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {

//        boolean renderedAlignment = false;
        boolean isInputElement = false;

        boolean isTDOrTH = GwtClientUtils.isTDorTH(element); // because canBeRenderedInTD can be true
        boolean isInput = isTagInput();
        boolean multiLine = isMultiLine();

        if (isInput && (isTDOrTH || isToolbarContainer(element))) { // needToRenderToolbarContent()
            isInputElement = true;
            SimpleTextBasedCellEditor.clearInputElement(element);
//            renderedAlignment = true;
        } else {
//            if(isTDOrTH || isInput) {
                clearRenderTextAlignment(property, element, isInput, renderContext.getRendererType());
//                renderedAlignment = true;
//            }

            if(isInput)
                isInputElement = true;

            CellRenderer.clearBasedTextFonts(property, element, renderContext);
            clearRender(property, element, renderContext, multiLine);
        }

        if(isInputElement)
            clearSimpleInputElement(element);

        clearPadding(getSizeElement(element));

        if (property.isEditableNotNull())
            element.removeClassName("text-based-value-required");

        element.removeClassName("text-based-value-null");
        element.removeClassName("text-based-value-empty");

        if(property.isEditableNotNull()) {
            Element inputElement = getSimpleInputElement(element);
            if(inputElement != null) {
                inputElement.removeClassName("is-invalid");
            }
        }

        element.removeClassName("text-based-value-multi-line");

        return true; // renderedAlignment;
    }

    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public int getWidthPadding() {
        return CELL_HORIZONTAL_PADDING;
    }

    @Override
    protected Object getExtraValue(UpdateContext updateContext) {
        return updateContext.getPlaceholder();
    }

    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        boolean isNull = value == null;
        String innerText = isNull ? null : format(value, updateContext.getRendererType());

        String title;
        title = property.echoSymbols ? "" : innerText;
        if(isNull) {
            element.addClassName("text-based-value-null");
            if(property.isEditableNotNull())
                title = REQUIRED_VALUE;
            innerText = "";
        } else {
            element.removeClassName("text-based-value-null");
            if(innerText.isEmpty()) {
                innerText = EMPTY_VALUE;
                element.addClassName("text-based-value-empty");
            } else
                element.removeClassName("text-based-value-empty");
        }

        element.setTitle(title);

        String placeholder = extraValue != null ? ((String) extraValue) : null;
        Element inputElement = getSimpleInputElement(element);
        if(inputElement != null) {
            assert isTagInput();
            if(property.isEditableNotNull()) {
                if (isNull) {
                    inputElement.addClassName("is-invalid");
                } else {
                    inputElement.removeClassName("is-invalid");
                }
            }
            SimpleTextBasedCellEditor.setInputValue(inputElement.cast(), innerText);
            if (placeholder != null)
                inputElement.setAttribute("placeholder", placeholder);
            else
                inputElement.removeAttribute("placeholder");
            return false;
        }

        if(innerText.contains("\n"))
            element.addClassName("text-based-value-multi-line");
        else
            element.removeClassName("text-based-value-multi-line");

        // important to make paste work (otherwise DataGrid.sinkPasteEvent cannot put empty selection), plus for sizing
        GwtClientUtils.setDataHtmlOrText(element, isNull ? (placeholder != null ? placeholder : EscapeUtils.UNICODE_NBSP) : innerText, false);
        return true;
    }

    public abstract String format(PValue value, RendererType rendererType);
}

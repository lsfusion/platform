package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.GFullInputType;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.InputBasedCellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public abstract class InputBasedCellRenderer extends CellRenderer {

    public InputBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    private static native JavaScriptObject getSimpleReadonlyFnc(InputElement element)/*-{
        return function(readonly) {
            $wnd.setDisabledNative(element, readonly != null && readonly);
            $wnd.setReadonlyNative(element, readonly != null && !readonly);
        }
    }-*/;

    public static void appendInputElement(Element cellParent, InputElement inputElement, boolean removeAllPMB, boolean isToolbarContainer, GInputType inputType) {
        if(removeAllPMB && inputType.isRemoveAllPMB())
            removeAllPMB(cellParent, inputElement);

        // should be before setupParent
        cellParent.appendChild(inputElement);

        if(isToolbarContainer || inputType.isStretch()) {
            // the problem of the height:100% is that: when it is set for the input element, line-height is ignored (and line-height is used for example in bootstrap, so when the data is drawn with the regular div, but input with the input element, they have different sizes, which is odd)
            // but flex works fine for the input element, but we cannot set display flex for the table cell element
            // so we'll use 100% for table cells (it's not that big deal for table cells, because usually there are a lot of cells without input in a row, and they will respect line-height)
            if (GwtClientUtils.isTDorTH(cellParent))
                GwtClientUtils.setupPercentParent(inputElement);
            else
                GwtClientUtils.setupFlexParent(inputElement);
        }
    }

    private final static String inputElementProp = "inputElement";
    private final static String inputElementTypeProp = "inputElementType";

    public static InputElement getInputElement(Element element) {
        return (InputElement) element.getPropertyObject(inputElementProp);
    }
    public static GInputType getInputElementType(Element element) {
        return (GInputType) element.getPropertyObject(inputElementTypeProp);
    }

    public static void setInputElement(Element element, InputElement inputElement, GInputType inputType) {
        element.setPropertyObject(inputElementProp, inputElement);
        inputElement.setPropertyObject(inputElementTypeProp, inputType);
        setReadonlyFnc(element, getSimpleReadonlyFnc(inputElement));
        setFocusElement(element, inputElement);
    }

    public static void clearInputElement(Element element) {
//        ((InputElement)element.getPropertyObject(inputElementProp)).setPropertyObject(inputElementTypeProp, null);
        element.setPropertyObject(inputElementProp, null);
        clearReadonlyFnc(element);
        clearFocusElement(element);
    }

    public static InputElement getInputEventTarget(Element parent, Event event) {
        InputElement inputElement = getInputElement(parent);
        if(inputElement == event.getEventTarget().cast())
            return inputElement;
        return null;
    }

    public static Element getFocusEventTarget(Element parent, Event event) {
        Object focusElement = getFocusElement(parent);
        if(focusElement == event.getEventTarget().cast())
            return (Element) focusElement;
        return null;
    }

    public static Element getMainElement(Element element) {
        InputElement inputElement = getInputElement(element);
        if(inputElement != null)
            return inputElement;
        return element;
    }

    public static Element getSizeElement(Element element) {
        if(isToolbarContainer(element)) {
            InputElement sizeElement = getInputElement(element);
            assert sizeElement != null;
            return sizeElement;
        }
        return element;
    }

    public static InputElement createInputElement(GPropertyDraw property, GFullInputType type) {
        GInputType inputType = type.inputType;
        InputElement inputElement = GwtClientUtils.createInputElement(inputType.getName());
        if(inputType.isNumber() && type.type instanceof GIntegralType)
            inputElement.setAttribute("step", ((GIntegralType) type.type).getStep());
        if (inputType.isMultilineText() && property.hasAutoHeight())
            autosizeTextarea(inputElement);
        return inputElement;
    }
    public static GFullInputType getInputType(GPropertyDraw property, RendererType rendererType) {
        // actually for edit should be "edit type", not render type
        GType renderType = property.getRenderType(rendererType);

        GInputType inputType = property.inputType;
        if(inputType == null || (rendererType != RendererType.CELL && property.differentValue)) {
            inputType = renderType.getValueInputType();
        }
        return new GFullInputType(inputType, renderType);
    }

    public static native void autosizeTextarea(InputElement input) /*-{
        input.autoSizeTextArea = true;
        $wnd.autosize(input);
    }-*/;
    public static native void updateAutosizeTextarea(InputElement input) /*-{
        if(input.autoSizeTextArea)
            $wnd.autosize.update(input);
    }-*/;

    private final static String toolbarContainerProp = "toolbarContainer";

    private static void setToolbarContainer(Element element) {
        element.setPropertyBoolean(toolbarContainerProp, true);
    }
    public static boolean isToolbarContainer(Element element) {
        return element.getPropertyBoolean(toolbarContainerProp);
    }


    @Override
    public boolean canBeRenderedInTD() {
        if(isTagInput()) // input / textareas has fixed sizes, so can be used with multiline fixed sizes
            return true;

        return super.canBeRenderedInTD();
    }

    // key event when the editing is not started
    public static boolean isInputKeyEvent(Event event, UpdateContext context, boolean isMultiLine) {
        return GKeyStroke.isInputKeyEvent(event, context.isNavigateInput(), isMultiLine);
    }

    @Override
    public Element createRenderElement(RendererType rendererType) {
        if(isTagInput()) {
            if(needToRenderToolbarContent()) { // for an input with a toolbar we have to wrap it in a div to draw a toolbar
                DivElement toolbarContainer = Document.get().createDivElement();
                GwtClientUtils.addClassName(toolbarContainer, "prop-input-w-toolbar");
                setToolbarContainer(toolbarContainer);
                return toolbarContainer;
            } else
                return InputBasedCellRenderer.createInputElement(property, getInputType(rendererType));
        }

        return super.createRenderElement(rendererType);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        InputElement inputElement = null;

        boolean isTDOrTH = GwtClientUtils.isTDorTH(element); // because canBeRenderedInTD can be true
        boolean isToolbarContainer = isToolbarContainer(element);
        boolean isInput = isTagInput();

        RendererType rendererType = renderContext.getRendererType();
        GFullInputType fullInputType = isInput ? getInputType(rendererType) : null;
        GInputType inputType = isInput ? fullInputType.inputType : null;

        if(isInput) {
            if (isTDOrTH || isToolbarContainer) {
                // assert isTDOrTH != isToolbarContainer(element);
                inputElement = InputBasedCellRenderer.createInputElement(property, fullInputType);
                appendInputElement(element, inputElement, renderContext.isInputRemoveAllPMB(), isToolbarContainer, inputType); // isTDorTH -> isInputRemoveAllPMB - for the filter
            } else
                inputElement = (InputElement) element;
        }

        // somewhy text alignment also works for input (but doesn't work for div), however:
        // there is some difference between align-items center (flex) and vertical align baseline / middle (text), and align items center seems to be more accurate (and better match input vertical align baseline / middle)
        boolean renderedAlignment = false;
        boolean isInputStretch = isInput && inputType.isStretch();
        if(isTDOrTH || isInputStretch) { // || property.ellipsis also the problem is that vertical-align works only for table-cell and inline displays, which is not what we have, so we can't render text alignment for regular divs to provide ellipsis for example
            // in theory when we have not stretched input but have alignment stretch, we could have used flex-alignment stretch, but in that case we would have to create extra div
            // actually when there is no text but stretch then it doesn't need to be set, but it won't break anything (and for fonts we don't do that)
            renderTextAlignment(isInputStretch ? inputElement : element, property.getHorzTextAlignment(), property.getVertTextAlignment());
            renderedAlignment = true;
        }

        if(inputElement != null)
            setInputElement(element, inputElement, inputType);

        return renderedAlignment;
    }

    protected GFullInputType getInputType(RendererType rendererType) {
        assert isTagInput();
        return getInputType(property, rendererType);
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
//        boolean renderedAlignment = false;

        boolean isTDOrTH = GwtClientUtils.isTDorTH(element); // because canBeRenderedInTD can be true
        boolean isInput = isTagInput();

        if (isInput && (isTDOrTH || isToolbarContainer(element))) { // needToRenderToolbarContent()
            InputBasedCellEditor.clearInputElement(element);
//            renderedAlignment = true;
        } else {
//            if(isTDOrTH || isInput) {
            clearRenderTextAlignment(element, property.getHorzTextAlignment(), property.getVertTextAlignment());
            //                renderedAlignment = true;
//            }
        }

        if(!isInput)
            GFormController.clearFont(element);

        if(isInput)
            clearInputElement(element);

        return true; // renderedAlignment;
    }
}

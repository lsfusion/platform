package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.classes.view.SimpleTextBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public abstract class TypeInputBasedCellRenderer extends InputBasedCellRenderer {

    public TypeInputBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean canBeRenderedInTD() {
        return true;
    }

    private final static String inputElementProp = "InputBasedElement";

    public static InputElement getInputElement(Element parent) {
        return (InputElement) parent.getPropertyObject(inputElementProp);
    }

    public static void setInputElement(Element element, InputElement inputElement) {
        element.setPropertyObject(inputElementProp, inputElement);
        SimpleTextBasedCellRenderer.setSimpleReadonlyFnc(element, inputElement);
    }
    public static void clearInputElement(Element element) {
        element.setPropertyObject(inputElementProp, null);
        SimpleTextBasedCellRenderer.clearSimpleReadonlyFnc(element);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        InputElement inputElement;

        boolean renderedAlignment = false;

        boolean isTDOrTH = GwtClientUtils.isTDorTH(element); // because canBeRenderedInTD can be true
        boolean isInput = isTagInput();
        if (!isInput || isTDOrTH || isToolbarContainer(element)) {
            inputElement = renderInputElement(element, renderContext);

            if(isTDOrTH) {
                renderTextAlignment(property, element, false, renderContext.getRendererType());

                renderedAlignment = true;
            }
        } else
            inputElement = (InputElement) element;

        setInputElement(element, inputElement);

        return renderedAlignment;
    }

    public InputElement renderInputElement(Element element, RenderContext renderContext) {
        InputElement inputElement = createInput(property, renderContext.getRendererType());
        element.appendChild(inputElement);
        return inputElement;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        clearInputElement(element);

        if(GwtClientUtils.isTDorTH(element))
            clearRenderTextAlignment(property, element, false, renderContext.getRendererType());

        return false;
    }
}

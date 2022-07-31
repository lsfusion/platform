package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.LabelWidget;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LogicalCellRenderer extends CellRenderer {

    private boolean threeState;

    public LogicalCellRenderer(GPropertyDraw property, boolean threeState) {
        super(property);
        this.threeState = threeState;
    }

    @Override
    public boolean canBeRenderedInTD() {
        return true;
    }

    private final static String inputElementProp = "logicalInputElement";

    public static InputElement getInputEventTarget(Element parent, Event event) {
        InputElement inputElement = getInputElement(parent);
        if(inputElement == event.getEventTarget().cast())
            return inputElement;
        return null;
    }

    public static InputElement getInputElement(Element parent) {
        return (InputElement) parent.getPropertyObject(inputElementProp);
    }

    public static void setInputElement(Element element, InputElement inputElement) {
        element.setPropertyObject(inputElementProp, inputElement);
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        InputElement inputElement;

        boolean renderedAlignment = false;

        boolean isTDOrTH = GwtClientUtils.isTDorTH(element); // because canBeRenderedInTD can be true
        boolean isInput = isTagInput();
        if (!isInput || isTDOrTH) {
            inputElement = renderInputElement(element);

            if(isTDOrTH) {
                renderTextAlignment(property, element);

                renderedAlignment = true;
            }
        } else
            inputElement = (InputElement) element;

        setInputElement(element, inputElement);

        return renderedAlignment;
    }

    public static InputElement renderInputElement(Element element) {
        InputElement inputElement = createCheckInput();
        element.appendChild(inputElement);
        return inputElement;
    }

    public static InputElement createCheckInput() {
        InputElement input = Document.get().createCheckInputElement();
        input.setTabIndex(-1);
        input.addClassName("logicalRendererCheckBox");
        input.addClassName("form-check-input"); // bootstrap
        return input;
    }

    @Override
    public void renderPanelLabel(LabelWidget label) {
        label.addStyleName("form-check-label");
    }

    @Override
    public void renderPanelContainer(SizedFlexPanel panel) {
        panel.addStyleName("form-check");
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        setInputElement(element, null);

        if(GwtClientUtils.isTDorTH(element))
            clearRenderTextAlignment(element);

        return false;
    }

    @Override
    public boolean updateContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        InputElement input = getInputElement(element);
        boolean newValue = value != null && (Boolean) value;
        setChecked(input, newValue);
        input.setDisabled(threeState && value == null);

        return false;
    }

    public static void setChecked(InputElement input, boolean newValue) {
        input.setChecked(newValue);
        input.setDefaultChecked(newValue);
    }

    public static void cancelChecked(InputElement input) {
        input.setChecked(input.isDefaultChecked());
    }

    @Override
    public Element createRenderElement() {
        if(isTagInput())
            return createCheckInput();

        return super.createRenderElement();
    }

    //    private String getCBImagePath(Object value) {
//        boolean checked = value != null && (Boolean) value;
//        return GwtClientUtils.getModuleImagePath("checkbox_" + (checked ? "checked" : "unchecked") + ".png");
//    }

    @Override
    public String format(Object value) {
        return (value != null) && ((Boolean) value) ? "TRUE" : "FALSE";
    }
}

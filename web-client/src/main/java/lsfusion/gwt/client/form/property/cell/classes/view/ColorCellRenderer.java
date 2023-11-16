package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.TypeInputBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class ColorCellRenderer extends TypeInputBasedCellRenderer {

    public ColorCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected InputElement createInput(GPropertyDraw property) {
        InputElement input = GwtClientUtils.createInputElement("color");
        input.addClassName("input-color"); //set opacity
        return input;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        element.addClassName("form-control");
        InputElement input = getInputElement(element);
        input.setValue(getColorValue(value));
        return false;
    }

    @Override
    protected String getBackground(UpdateContext updateContext) {
        String colorValue = getColorValue(updateContext.getValue());
        if (colorValue != null)
            return colorValue;

        return super.getBackground(updateContext);
    }

    private String getColorValue(PValue value) {
        return PValue.getColorStringValue(value);
    }

    @Override
    public String format(PValue value) {
        return getColorValue(value);
    }
}

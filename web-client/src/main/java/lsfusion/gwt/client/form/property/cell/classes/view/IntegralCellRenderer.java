package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.classes.GFullInputType;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class IntegralCellRenderer extends FormatCellRenderer<Number> {
    public IntegralCellRenderer(GPropertyDraw property) {
        super(property);
    }

    protected boolean isNative(GFullInputType fullInputType) {
        return fullInputType.inputType.isNumber();
    }
}

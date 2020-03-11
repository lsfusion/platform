package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class NumberGridCellRenderer extends FormatGridCellRenderer<Number, NumberFormat> {
    public NumberGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String castToString(Number value) {
        return format.format(value);
    }
}

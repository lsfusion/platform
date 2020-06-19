package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class NumberCellRenderer extends FormatCellRenderer<Number, NumberFormat> {
    public NumberCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(Number value) {
        return format.format(value);
    }
}

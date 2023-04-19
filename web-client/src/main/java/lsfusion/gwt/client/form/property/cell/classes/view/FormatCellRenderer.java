package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public abstract class FormatCellRenderer<T> extends SimpleTextBasedCellRenderer {

    protected GFormatType getFormatType() {
        return property.getFormatType();
    }

    @Override
    public String format(PValue value) {
        return getFormatType().formatString(value, property.pattern);
    }

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
    }
}

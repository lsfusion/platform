package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FormatCellRenderer<T> extends TextBasedCellRenderer<T> {

    protected GFormatType getFormatType() {
        return property.getFormatType();
    }

    @Override
    public String format(T value) {
        return getFormatType().formatString(value, property.pattern);
    }

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
    }
}

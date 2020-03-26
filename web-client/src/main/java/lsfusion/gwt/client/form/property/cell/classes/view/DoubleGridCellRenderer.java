package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.form.property.GPropertyDraw;

public class DoubleGridCellRenderer extends NumberGridCellRenderer {
    public DoubleGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String castToString(Number value) {
        assert value != null;
        return format.format(new Double(value.toString()));
    }
}

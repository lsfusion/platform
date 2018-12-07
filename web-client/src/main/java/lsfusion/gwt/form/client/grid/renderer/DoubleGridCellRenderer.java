package lsfusion.gwt.form.client.grid.renderer;

import lsfusion.gwt.form.shared.view.GPropertyDraw;

import java.math.BigDecimal;

public class DoubleGridCellRenderer extends NumberGridCellRenderer {
    public DoubleGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String renderToString(Number value) {
        assert value != null;
        return format.format(new BigDecimal(value.toString()));
    }
}

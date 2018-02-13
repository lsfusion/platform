package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

import java.math.BigDecimal;

public class DoubleGridCellRenderer extends NumberGridCellRenderer {
    public DoubleGridCellRenderer(GPropertyDraw property, NumberFormat format) {
        super(property, format);
    }

    @Override
    protected String renderToString(Number value) {
        assert value != null;
        return format.format(new BigDecimal(value.toString()));
    }
}

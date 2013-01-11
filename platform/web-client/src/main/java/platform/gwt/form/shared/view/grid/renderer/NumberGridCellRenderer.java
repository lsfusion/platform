package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import platform.gwt.form.shared.view.GPropertyDraw;

public class NumberGridCellRenderer extends TextBasedGridCellRenderer<Number> {
    private final NumberFormat format;

    public NumberGridCellRenderer(GPropertyDraw property) {
        this(property, NumberFormat.getDecimalFormat());
    }

    public NumberGridCellRenderer(GPropertyDraw property, NumberFormat format) {
        super(property, Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Number value) {
        return format.format(value);
    }
}

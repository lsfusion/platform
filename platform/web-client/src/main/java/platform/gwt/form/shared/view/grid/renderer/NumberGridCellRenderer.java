package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;

public class NumberGridCellRenderer extends TextGridCellRenderer<Number> {
    private final NumberFormat format;

    public NumberGridCellRenderer() {
        this(NumberFormat.getDecimalFormat());
    }

    public NumberGridCellRenderer(NumberFormat format) {
        super(Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Number value) {
//        return format.format(value);
        //так быстрей...
        return String.valueOf(value);
    }
}

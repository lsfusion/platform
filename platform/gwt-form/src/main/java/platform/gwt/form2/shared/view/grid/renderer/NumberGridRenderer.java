package platform.gwt.form2.shared.view.grid.renderer;

import com.google.gwt.i18n.client.NumberFormat;

public class NumberGridRenderer extends SafeHtmlGridRenderer<Number> {
    private final NumberFormat format;

    public NumberGridRenderer() {
        this(NumberFormat.getDecimalFormat());
    }

    public NumberGridRenderer(NumberFormat format) {
        this.format = format;
    }

    @Override
    protected String renderToString(Number value) {
        return format.format(value);
    }
}

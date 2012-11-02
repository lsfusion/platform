package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;

public class NumberGridRenderer extends SafeHtmlGridRenderer<Number> {
    private final NumberFormat format;

    public NumberGridRenderer() {
        this(NumberFormat.getDecimalFormat());
    }

    public NumberGridRenderer(NumberFormat format) {
        super(Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Number value) {
        return format.format(value);
    }
}

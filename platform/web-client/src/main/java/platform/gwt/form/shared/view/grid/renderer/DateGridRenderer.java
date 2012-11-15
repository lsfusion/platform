package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.base.shared.GwtSharedUtils;

import java.util.Date;

public class DateGridRenderer extends SafeHtmlGridRenderer<Date> {
    private final DateTimeFormat format;

    public DateGridRenderer() {
        this(GwtSharedUtils.getDefaultDateFormat());
    }

    public DateGridRenderer(DateTimeFormat format) {
        super(Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Date value) {
        return format.format(value, null);
    }
}

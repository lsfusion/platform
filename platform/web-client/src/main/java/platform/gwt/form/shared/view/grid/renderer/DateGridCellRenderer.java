package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;

import java.util.Date;

public class DateGridCellRenderer extends TextBasedGridCellRenderer<Object> {
    private final DateTimeFormat format;

    public DateGridCellRenderer(GPropertyDraw property) {
        this(property, GwtSharedUtils.getDefaultDateFormat());
    }

    public DateGridCellRenderer(GPropertyDraw property, DateTimeFormat format) {
        super(property, Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Object value) {
        return format.format((Date) value);
    }

    @Override
    protected void setInnerText(DivElement div, String innerText) {
        if (innerText == null) {
            div.setInnerText(EscapeUtils.UNICODE_NBSP);
        } else {
            div.setInnerText(innerText);
        }
    }
}

package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.DateCellRenderer;

import java.text.ParseException;
import java.util.Date;

public abstract class GADateType extends GFormatType {

    @Override
    public DateCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateCellRenderer(property);
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }

    private static Date wideFormattableDateTime = null;

    @Override
    protected Object getDefaultWidthValue() {
        if(wideFormattableDateTime == null)
            wideFormattableDateTime = com.google.gwt.i18n.shared.DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").parse("1991-11-21 10:55:55");
        return fromDate(wideFormattableDateTime);
    }

    @Override
    public Object parseString(String value, String pattern) throws ParseException {
        return value.isEmpty() ? null : fromDate(GDateType.parseDate(value, getFormats(pattern)));
    }

    @Override
    public String formatString(Object value, String pattern) {
        return value == null ? "" : getFormat(pattern).format(toDate(value));
    }

    // "extended" getFormat + some extra formates
    protected DateTimeFormat[] getFormats(String pattern) {
        return new DateTimeFormat[] {getFormat(pattern)};
    }

    public abstract DateTimeFormat getFormat(String pattern);

    public abstract Object fromDate(Date date);

    public abstract Date toDate(Object value);
}

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
    public Object parseString(String value, String pattern, boolean edit) throws ParseException {
        return value.isEmpty() ? null : fromDate(GDateType.parseDate(value, getFormats(pattern, edit)));
    }

    @Override
    public String formatString(Object value, String pattern, boolean edit) {
        return value == null ? "" : getFormat(pattern, edit).format(toDate(value));
    }

    // "extended" getFormat + some extra formates
    protected DateTimeFormat[] getFormats(String pattern, boolean edit) {
        return new DateTimeFormat[] {getFormat(pattern, edit)};
    }

    protected abstract DateTimeFormat getFormat(String pattern, boolean edit);

    public abstract Object fromDate(Date date);

    public abstract Date toDate(Object value);
}

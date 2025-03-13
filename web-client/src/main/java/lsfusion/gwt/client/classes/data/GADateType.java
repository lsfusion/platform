package lsfusion.gwt.client.classes.data;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.DateCellRenderer;

import java.text.ParseException;
import java.util.Date;

public abstract class GADateType extends GFormatType {

    @Override
    public DateCellRenderer createCellRenderer(GPropertyDraw property) {
        return new DateCellRenderer(property, this);
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }

    private static transient JsDate wideFormattableDateTime = null;

    @Override
    protected PValue getDefaultWidthValue() {
        if(wideFormattableDateTime == null)
            wideFormattableDateTime = GwtClientUtils.createJsDate(1991,11,21,10,55,55,555);
        return fromJsDate(wideFormattableDateTime);
    }

    @Override
    public PValue parseString(String value, String pattern) throws ParseException {
        return parseString(value, getFormats(pattern));
    }

    @Override
    public String formatString(PValue value, String pattern) {
        return formatString(value, getFormat(pattern));
    }

    public PValue parseISOString(String value) throws ParseException {
        return parseString(value, getISOFormat());
    }

    public String formatISOString(PValue value) {
        return formatString(value, getISOFormat());
    }

    private String formatString(PValue value, DateTimeFormat format) {
        JsDate date = toJsDate(value);
        return format.format(date == null ? null : new Date(Math.round(date.getTime())));
    }

    private PValue parseString(String value, DateTimeFormat... formats) throws ParseException {
        if (value.isEmpty())
            return null;

        Date date = GDateType.parseDate(value, formats);
        return fromJsDate(date != null ? GwtClientUtils.createJsDate(date.getTime()) : null);
    }

    // "extended" getFormat + some extra formates
    protected DateTimeFormat[] getFormats(String pattern) {
        return new DateTimeFormat[] {getFormat(pattern)};
    }

    public abstract DateTimeFormat getFormat(String pattern);
    public abstract DateTimeFormat getISOFormat(); // format to be used in input date / datetime-local / time

    public abstract JsDate toJsDate(PValue value);

    public abstract PValue fromJsDate(JsDate date);
}
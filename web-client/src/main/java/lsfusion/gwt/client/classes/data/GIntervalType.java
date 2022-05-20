package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntervalCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.IntervalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

public abstract class GIntervalType extends GFormatType {

    public static GIntervalType getInstance(String type) {
        switch (type) {
            case "DATE":
                return GDateIntervalType.instance;
            case "TIME":
                return GTimeIntervalType.instance;
            case "DATETIME":
                return GDateTimeIntervalType.instance;
            case "ZDATETIME":
                return GZDateTimeIntervalType.instance;
        }
        return null;
    }

    protected boolean isSingleLocal() {
        return true;
    }

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new IntervalCellRenderer(property);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new IntervalCellEditor(editManager, editProperty, getIntervalType(), this);
    }

    @Override
    protected Object getDefaultWidthValue() {
        return new BigDecimal("1636629071.1636629071");
    }

    protected Long parse(String date, String pattern) throws ParseException {
        GADateType timeSeriesType = getTimeSeriesType();
        return fromDate(timeSeriesType.toDate(timeSeriesType.parseString(date, pattern)));
    }
    protected String format(Long epoch, String pattern) {
        GADateType timeSeriesType = getTimeSeriesType();
        return timeSeriesType.formatString(timeSeriesType.fromDate(toDate(epoch)), pattern);
    }
    public DateTimeFormat getSingleFormat(String pattern) {
        return getTimeSeriesType().getFormat(pattern);
    }

    protected abstract GADateType getTimeSeriesType();

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        if(s.isEmpty())
            return null;

        return GwtClientUtils.parseInterval(s, date -> parse(date, pattern));
    }

    @Override
    public String formatString(Object value, String pattern) {
        if(value == null)
            return null;

        return GwtClientUtils.formatInterval(value, epoch -> format(epoch, pattern));
    }

    public abstract String getIntervalType();

    public Date toDate(Object value, boolean from) {
        if(value == null)
            return new Date();

        return toDate(getEpoch(value, from));
    }

    private static transient TimeZone UTCZone = TimeZone.createTimeZone(0);
    protected Date toDate(long epoch) {
        Date date = new Date(epoch);
        boolean local = isSingleLocal();
        if(local) { // here is tricky for local dates we convert to string (to get "absolute" params, and then parsing back)
            DateTimeFormat format = getSingleFormat(null);// we don't care about the pattern
            return format.parse(format.format(date, UTCZone));
        }

        return date;
    }

    protected long fromDate(Date date) {
        boolean local = isSingleLocal();
        if(local) { // here is tricky for local dates we convert to string (to get "absolute" params, and then parsing back)
//            DateTimeFormat format = getSingleFormat(null);// we don't care about the pattern
//            date = format.parse(format.format(date), UTCZone);
            date = GwtClientUtils.fromJsDate(GwtClientUtils.getUTCDate(1900 + date.getYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds()));
        }
        return date.getTime();
    }

    public Object fromDate(Date from, Date to) {
        if(from == null || to == null)
            return null;

        return new BigDecimal(fromDate(from) + "." + fromDate(to));
    }

    public static long getEpoch(Object value, boolean from) {
        assert value instanceof BigDecimal;
        String object = String.valueOf(value);
        int indexOfDecimal = object.indexOf(".");
        return Long.parseLong(from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1));
    }
}

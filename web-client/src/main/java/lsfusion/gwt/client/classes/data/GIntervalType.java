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

    public abstract DateTimeFormat getSingleFormat(String pattern);

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

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        if(s.isEmpty())
            return null;

        String[] parts = s.split(" - ");
        DateTimeFormat singleFormat = getSingleFormat(pattern);
        return fromDate(singleFormat.parse(parts[0]), singleFormat.parse(parts[1]));
    }

    private static transient TimeZone UTCZone = TimeZone.createTimeZone(0);
    private static String format(DateTimeFormat format, boolean isLocal, Date utcDate) {
        if(isLocal)
            return format.format(utcDate, UTCZone); // formatting in utc

        return format.format(utcDate);
    }

    @Override
    public String formatString(Object value, String pattern, boolean edit) {
        if(value == null)
            return null;

        DateTimeFormat singleFormat = getSingleFormat(pattern);
        boolean local = isSingleLocal();
        return format(singleFormat, local, getUTCDate(value, true)) + " - " + format(singleFormat, local, getUTCDate(value, false));
    }

    public abstract String getIntervalType();


    public Date toDate(Object value, boolean from) {
        if(value == null)
            return new Date();

        Date utcDate = getUTCDate(value, from);
        boolean local = isSingleLocal();
        if(local) { // here is tricky for local dates we convert to string (to get "absolute" params, and then parsing back)
            DateTimeFormat format = getSingleFormat(null);// we don't care about the pattern
            return format.parse(format.format(utcDate, UTCZone));
        }

        return utcDate;
    }

    private long fromDate(Date date) {
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

    public static Date getUTCDate(Object value, boolean from) {
        assert value instanceof BigDecimal;
        String object = String.valueOf(value);
        int indexOfDecimal = object.indexOf(".");
        return new Date(Long.parseLong(from ? object.substring(0, indexOfDecimal) : object.substring(indexOfDecimal + 1)));
    }
}

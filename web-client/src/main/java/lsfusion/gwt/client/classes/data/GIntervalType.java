package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntervalCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.IntervalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

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
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new IntervalCellRenderer(property);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new IntervalCellEditor(editManager, editProperty, getIntervalType(), this);
    }

    @Override
    protected PValue getDefaultWidthValue() {
        return PValue.getPValue(1636629071L, 1636629071L);
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
    public PValue parseString(String s, String pattern) throws ParseException {
        if(s.isEmpty())
            return null;

        PValue parsedObject;
        try {
            parsedObject = GwtClientUtils.parseInterval(s, date -> GIntervalType.this.parse(date, pattern));
            if (parsedObject == null)
                throw new Exception();
        } catch (Exception e) {
            throw new ParseException(e.getMessage(), 0);
        }

        return parsedObject;
    }

    @Override
    public String formatString(PValue value, String pattern) {
        return GwtClientUtils.formatInterval(value, epoch -> format(epoch, pattern));
    }

    public abstract String getIntervalType();

    public Date toDate(PValue value, boolean from) {
        if(value == null)
            return new Date();

        return toDate(PValue.getIntervalValue(value, from));
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

    public PValue fromDate(Date from, Date to) {
        if(from == null || to == null)
            return null;

        return PValue.getPValue(fromDate(from), fromDate(to));
    }

}

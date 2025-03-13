package lsfusion.gwt.client.classes.data;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.i18n.client.DateTimeFormat;
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
        return PValue.getPValue(25220229000L, 25220229000L);
    }

    protected Long parse(String date, String pattern) throws ParseException {
        GADateType timeSeriesType = getTimeSeriesType();
        return fromJsDate(timeSeriesType.toJsDate(timeSeriesType.parseString(date, pattern)));
    }
    protected String format(Long epoch, String pattern) {
        GADateType timeSeriesType = getTimeSeriesType();
        return timeSeriesType.formatString(timeSeriesType.fromJsDate(toJsDate(epoch)), pattern);
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

    public JsDate toJsDate(PValue value, boolean from) {
        if(value == null)
            return GwtClientUtils.createJsDate();

        return toJsDate(PValue.getIntervalValue(value, from));
    }

//    private static transient TimeZone UTCZone = TimeZone.createTimeZone(0);
    protected JsDate toJsDate(long epoch) {
        JsDate date = GwtClientUtils.createJsDate((double) epoch);

        boolean local = isSingleLocal();
        if(local)
            date = GwtClientUtils.createJsDate(GwtClientUtils.getUTCYear(date), GwtClientUtils.getUTCMonth(date), GwtClientUtils.getUTCDate(date),
                    GwtClientUtils.getUTCHours(date), GwtClientUtils.getUTCMinutes(date), GwtClientUtils.getUTCSeconds(date), GwtClientUtils.getUTCMilliseconds(date));

        return date;
    }

    protected long fromJsDate(JsDate date) {
        boolean local = isSingleLocal();
        if(local)
            date = GwtClientUtils.createJsUTCDate(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds(), date.getMilliseconds());

        return (long) date.getTime();
    }

    public PValue fromDate(JsDate from, JsDate to) {
        if(from == null || to == null)
            return null;

        return PValue.getPValue(fromJsDate(from), fromJsDate(to));
    }

}

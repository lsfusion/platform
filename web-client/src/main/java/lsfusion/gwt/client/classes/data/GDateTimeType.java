package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.shared.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateTimeGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DateGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GDateTimeType extends GFormatType<com.google.gwt.i18n.client.DateTimeFormat> {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public com.google.gwt.i18n.client.DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getDateTimeFormat(pattern);
    }

    @Override
    public AbstractGridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateGridCellRenderer(property);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateTimeGridCellEditor(editManager, editProperty);
    }

    @Override
    public Timestamp parseString(String value, String pattern) throws ParseException {
        if (value.isEmpty()) {
            return null;
        }
        Date date = GDateType.parseDate(value, getDefaultDateTimeFormat(), getDefaultDateTimeShortFormat(), getDefaultDateFormat());
        return new Timestamp(date.getTime());
    }

    private static Date wideFormattableDateTime = null;

    public static Date getWideFormattableDateTime() {
        if(wideFormattableDateTime == null)
            wideFormattableDateTime = DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").parse("1991-11-21 10:55:55");
        return wideFormattableDateTime;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return getWideFormattableDateTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateTimeCaption();
    }

    @Override
    public GEditBindingMap.EditEventFilter getEditEventFilter() {
        return GEditBindingMap.numberEventFilter;
    }
}

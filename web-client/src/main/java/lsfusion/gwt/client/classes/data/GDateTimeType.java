package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.shared.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DateCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

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
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DateCellRenderer(property);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DateTimeCellEditor(editManager, editProperty);
    }

    @Override
    public Object parseString(String value, String pattern) throws ParseException {
        return value.isEmpty() ? null : GDateTimeDTO.fromDate(GDateType.parseDate(value, getDefaultDateTimeFormat(), getDefaultDateTimeShortFormat(), getDefaultDateFormat()));
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

    @Override
    public boolean isId() {
        return false;
    }
}

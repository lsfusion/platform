package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GDateTimeType extends GADateType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    protected com.google.gwt.i18n.client.DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern, false);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new DateTimeCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern) {
        return new DateTimeFormat[] { getDateTimeFormat(pattern, true), getDefaultDateTimeShortFormat(), getDefaultDateFormat(true) };
    }

    @Override
    public Object fromDate(Date date) {
        return GDateTimeDTO.fromDate(date);
    }

    @Override
    public Date toDate(Object value) {
        return ((GDateTimeDTO) value).toDateTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateTimeCaption();
    }
}

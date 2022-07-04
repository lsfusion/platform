package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GDateTimeType extends GADateType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public com.google.gwt.i18n.client.DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern);
    }

    @Override
    public CellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new DateTimeCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern) {
        return GwtClientUtils.add(super.getFormats(pattern), new DateTimeFormat[] { getDefaultDateTimeShortFormat(), getDefaultDateTimeFormat() }, DateTimeFormat[]::new);
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

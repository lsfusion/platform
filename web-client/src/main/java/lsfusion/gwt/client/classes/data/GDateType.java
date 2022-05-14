package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;

import java.text.ParseException;
import java.util.Date;

public class GDateType extends GADateType {

    public static GDateType instance = new GDateType();

    @Override
    protected DateTimeFormat getFormat(String pattern, boolean edit) {
        return GwtSharedUtils.getDateFormat(pattern, edit);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new DateCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern, boolean edit) {
        return GwtClientUtils.add(super.getFormats(pattern, edit), new DateTimeFormat[] { GwtSharedUtils.getDefaultDateFormat(edit) }, DateTimeFormat[]::new);
    }

    @Override
    public Object fromDate(Date date) {
        return GDateDTO.fromDate(date);
    }

    @Override
    public Date toDate(Object value) {
        return ((GDateDTO) value).toDate();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateCaption();
    }

    public static Date parseDate(String value, DateTimeFormat... formats) throws ParseException {
        for (DateTimeFormat format : formats) {
            try {
                return format.parseStrict(value);
            } catch (IllegalArgumentException ignore) {
            }
        }
        throw new ParseException("string " + value + "can not be converted to date", 0);
    }

}

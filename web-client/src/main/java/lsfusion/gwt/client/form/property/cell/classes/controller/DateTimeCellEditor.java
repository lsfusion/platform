package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.classes.data.GDateTimeType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class DateTimeCellEditor extends DateCellEditor {
    private static final DateTimeFormat format = GwtSharedUtils.getDefaultDateTimeFormat(true);

    public DateTimeCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected String formatToString(Date date) {
        return format.format(date);
    }

    @Override
    protected Date valueAsDate(Object value) {
        if (value == null) {
            return null;
        }
        return ((GDateTimeDTO) value).toDateTime();
    }

    @Override
    protected void onDateChanged(ValueChangeEvent<Date> event) {
        Date value = datePicker.getValue();
        value.setHours(0);
        value.setMinutes(0);
        value.setSeconds(0);
        editBox.setValue(format.format(value));
        editBox.getElement().focus();
    }

    protected Object parseString(String value) throws ParseException {
        return GDateTimeType.instance.parseString(value, property.pattern);
    }
}

package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.classes.GDateTimeType;
import lsfusion.gwt.form.shared.view.grid.EditManager;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;

public class DateTimeGridCellEditor extends DateGridCellEditor {
    private static final DateTimeFormat format = GwtSharedUtils.getDefaultDateTimeFormat();

    public DateTimeGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected String formatToString(Date date) {
        return format.format(date);
    }

    @Override
    protected Date valueAsDate(Object value) {
        return (Date) value;
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

    protected Timestamp parseString(String value) throws ParseException {
        return GDateTimeType.instance.parseString(value);
    }
}

package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.base.DateConverter;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.beans.PropertyChangeEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateTimePropertyEditor extends DatePropertyEditor {

    public DateTimePropertyEditor(Object value, SimpleDateFormat format, ClientPropertyDraw property) {
        super(value, format, property);
    }

    @Override
    public Date valueToDate(Object value) {
        return value instanceof Timestamp ? DateConverter.stampToDate((Timestamp) value) : null;
    }

    @Override
    public Object dateToValue(Date date) {
        return DateConverter.dateToStamp(date);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("day")) {
            if (popup.isVisible()) {
                dateSelected = true;
                popup.setVisible(false);
                Calendar calendar = jcalendar.getCalendar();
                setCalendar(new GregorianCalendar(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0));
            }
        } else {
            super.propertyChange(evt);
        }
    }
}

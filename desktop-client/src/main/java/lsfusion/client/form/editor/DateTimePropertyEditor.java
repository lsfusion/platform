package lsfusion.client.form.editor;

import lsfusion.base.DateConverter;
import lsfusion.interop.ComponentDesign;

import java.beans.PropertyChangeEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DateTimePropertyEditor extends DatePropertyEditor {

    public DateTimePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        super(value, format, design);
    }

    @Override
    public Date valueToDate(Object value) {
        return DateConverter.stampToDate((Timestamp) value);
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

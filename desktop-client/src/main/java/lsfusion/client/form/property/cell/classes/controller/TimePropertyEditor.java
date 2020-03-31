package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Date;

import static lsfusion.base.DateConverter.dateToTime;
import static lsfusion.base.DateConverter.timeToDate;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;
import static lsfusion.base.TimeConverter.sqlTimeToLocalTime;

public class TimePropertyEditor extends DatePropertyEditor {
    public TimePropertyEditor(Object value, SimpleDateFormat format, ClientPropertyDraw property) {
        super(value, format, property);
        calendarButton.setVisible(false);
    }

    @Override
    public Date valueToDate(Object value) {
        return value instanceof LocalTime ? timeToDate(localTimeToSqlTime((LocalTime) value)) : null;
    }

    @Override
    public Object dateToValue(Date date) {
        return sqlTimeToLocalTime(dateToTime(date));
    }
}

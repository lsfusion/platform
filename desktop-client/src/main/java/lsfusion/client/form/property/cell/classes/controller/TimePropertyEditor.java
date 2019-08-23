package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.base.DateConverter;
import lsfusion.interop.form.design.ComponentDesign;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimePropertyEditor extends DatePropertyEditor {
    public TimePropertyEditor(Object value, SimpleDateFormat format, ComponentDesign design) {
        super(value, format, design);
        calendarButton.setVisible(false);
    }

    @Override
    public Date valueToDate(Object value) {
        return value instanceof Time ? DateConverter.timeToDate((Time) value) : null;
    }

    @Override
    public Object dateToValue(Date date) {
        return DateConverter.dateToTime(date);
    }
}

package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import static lsfusion.base.DateConverter.*;

public class ZDateTimePropertyEditor extends DateTimePropertyEditor {

    public ZDateTimePropertyEditor(Object value, SimpleDateFormat format, ClientPropertyDraw property) {
        super(value, format, property);
    }

    @Override
    public Date valueToDate(Object value) {
        return value instanceof Instant ? stampToDate(instantToSqlTimestamp((Instant) value)) : null;
    }

    @Override
    public Object dateToValue(Date date) {
        return sqlTimestampToInstant(dateToStamp(date));
    }
}

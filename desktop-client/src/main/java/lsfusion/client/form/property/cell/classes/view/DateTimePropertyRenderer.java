package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.time.LocalDateTime;

import static lsfusion.base.DateConverter.localDateTimeToSqlTimestamp;
import static lsfusion.base.DateConverter.stampToDate;

public class DateTimePropertyRenderer extends FormatPropertyRenderer {

    public DateTimePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return stampToDate(localDateTimeToSqlTimestamp((LocalDateTime) value));
    }
}

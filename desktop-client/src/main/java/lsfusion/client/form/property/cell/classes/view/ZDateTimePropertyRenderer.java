package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.time.Instant;

import static lsfusion.base.DateConverter.instantToSqlTimestamp;
import static lsfusion.base.DateConverter.stampToDate;

public class ZDateTimePropertyRenderer extends DateTimePropertyRenderer {

    public ZDateTimePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return stampToDate(instantToSqlTimestamp((Instant) value));
    }
}

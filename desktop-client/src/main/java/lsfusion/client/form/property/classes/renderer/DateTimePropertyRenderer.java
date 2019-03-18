package lsfusion.client.form.property.classes.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.logics.ClientPropertyDraw;

import java.sql.Timestamp;

public class DateTimePropertyRenderer extends FormatPropertyRenderer {

    public DateTimePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return DateConverter.stampToDate((Timestamp) value);
    }
}

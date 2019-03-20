package lsfusion.client.form.property.cell.classes.view;

import lsfusion.base.DateConverter;
import lsfusion.client.form.property.ClientPropertyDraw;

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

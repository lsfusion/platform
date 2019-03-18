package lsfusion.client.form.property.classes.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.form.property.ClientPropertyDraw;

public class DatePropertyRenderer extends FormatPropertyRenderer {

    public DatePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return DateConverter.sqlToDate((java.sql.Date) value);
    }
}

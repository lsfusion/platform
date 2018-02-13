package lsfusion.client.form.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;

public class DatePropertyRenderer extends FormatPropertyRenderer {

    public DatePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return DateConverter.sqlToDate((java.sql.Date) value);
    }
}

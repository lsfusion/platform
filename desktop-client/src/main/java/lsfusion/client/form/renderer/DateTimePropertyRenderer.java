package lsfusion.client.form.renderer;

import lsfusion.base.DateConverter;
import lsfusion.client.logics.ClientPropertyDraw;

import javax.swing.*;
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

package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.time.LocalDate;

import static lsfusion.base.DateConverter.localDateToSqlDate;

public class DatePropertyRenderer extends FormatPropertyRenderer {

    public DatePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return localDateToSqlDate((LocalDate) value);
    }
}

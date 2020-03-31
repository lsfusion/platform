package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;

import java.time.LocalTime;

import static lsfusion.base.TimeConverter.localTimeToSqlTime;

public class TimePropertyRenderer extends FormatPropertyRenderer {
    public TimePropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    @Override
    protected Object preformat(Object value) {
        return localTimeToSqlTime((LocalTime) value);
    }
}

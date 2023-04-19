package lsfusion.gwt.client.form.property.cell.classes;

import lsfusion.gwt.client.form.property.PValue;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class GZDateTimeDTO implements Serializable {
    public long instant;

    @SuppressWarnings("UnusedDeclaration")
    public GZDateTimeDTO() {}

    public GZDateTimeDTO(long instant) {
        this.instant = instant;
    }

    public static PValue fromDate(Date date) {
        return PValue.getPValue(new GZDateTimeDTO(date.getTime()));
    }

    public Timestamp toDateTime() {
        return new Timestamp(instant);
    }

    @Override
    public String toString() {
        return toDateTime().toString();
    }
}

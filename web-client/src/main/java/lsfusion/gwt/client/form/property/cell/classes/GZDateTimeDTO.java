package lsfusion.gwt.client.form.property.cell.classes;

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

    public static GZDateTimeDTO fromDate(Date date) {
        return new GZDateTimeDTO(date.getTime());
    }

    public Timestamp toDateTime() {
        return new Timestamp(instant);
    }

    @Override
    public String toString() {
        return toDateTime().toString();
    }
}

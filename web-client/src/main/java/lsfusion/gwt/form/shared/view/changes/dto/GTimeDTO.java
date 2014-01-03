package lsfusion.gwt.form.shared.view.changes.dto;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

public class GTimeDTO implements Serializable {
    public int hour;
    public int minute;
    public int second;

    @SuppressWarnings("UnusedDeclaration")
    public GTimeDTO() {}

    public GTimeDTO(int hour, int minute, int second) {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }

    public Time toTime() {
        return new Time(hour, minute, second);
    }

    public static GTimeDTO fromDate(Date date) {
        return new GTimeDTO(date.getHours(), date.getMinutes(), date.getSeconds());
    }
}

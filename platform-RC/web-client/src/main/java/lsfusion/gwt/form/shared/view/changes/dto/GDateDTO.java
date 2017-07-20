package lsfusion.gwt.form.shared.view.changes.dto;

import java.io.Serializable;
import java.util.Date;

public class GDateDTO implements Serializable {
    public int day;
    public int month;
    public int year;

    @SuppressWarnings("UnusedDeclaration")
    public GDateDTO() {}

    public GDateDTO(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public Date toDate() {
        return new java.sql.Date(year, month, day);
    }

    public static GDateDTO fromDate(Date date) {
        return new GDateDTO(date.getDate(), date.getMonth(), date.getYear());
    }
}

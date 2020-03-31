package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class GDateDTO implements Serializable {
    public int year;
    public int month;
    public int day;

    @SuppressWarnings("UnusedDeclaration")
    public GDateDTO() {}

    public GDateDTO(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static GDateDTO fromDate(Date date) {
        return new GDateDTO(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
    }

    public Date toDate() {
        return new java.sql.Date(year - 1900, month - 1, day);
    }
    
    @Override
    public String toString() {
        return toDate().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GDateDTO)) return false;
        GDateDTO gDateDTO = (GDateDTO) o;
        return year == gDateDTO.year && month == gDateDTO.month && day == gDateDTO.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, day);
    }
}

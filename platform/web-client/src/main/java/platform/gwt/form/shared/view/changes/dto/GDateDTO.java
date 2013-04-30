package platform.gwt.form.shared.view.changes.dto;

import java.io.Serializable;

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
}

package lsfusion.gwt.client.form.object.table.grid.view;

public class CalendarDateBinding {

    public final String startFieldName;
    public final String endFieldName;
    public final boolean dateTime;

    private CalendarDateBinding(String startFieldName, String endFieldName, boolean dateTime) {
        this.startFieldName = startFieldName;
        this.endFieldName = endFieldName;
        this.dateTime = dateTime;
    }

    public static CalendarDateBinding date(String startFieldName, String endFieldName) {
        return new CalendarDateBinding(startFieldName, endFieldName, false);
    }

    public static CalendarDateBinding dateTime(String startFieldName, String endFieldName) {
        return new CalendarDateBinding(startFieldName, endFieldName, true);
    }

    public boolean isAllDay() {
        return !dateTime;
    }

    public boolean isDateTime() {
        return dateTime;
    }
}

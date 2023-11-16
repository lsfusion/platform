package lsfusion.interop.form.object.table.grid;

public enum ListViewType {
    GRID, PIVOT, MAP, CUSTOM, CALENDAR;

    // should match RendererType
    public boolean isValue() {
        return this == PIVOT;
    }

    public static ListViewType DEFAULT = GRID;

    // should correspond ListViewType in System.lsf
    public String getObjectName() {
        switch (this) {
            case GRID:
                return "grid";
            case PIVOT:
                return "pivot";
            case MAP:
                return "map";
            case CUSTOM:
                return "custom";
            case CALENDAR:
                return "calendar";
        }
        throw new UnsupportedOperationException();
    }

    public boolean isPivot() {
        return this == PIVOT;
    }

    public boolean isMap() {
        return this == MAP;
    }

    public boolean isCalendar() {
        return this == CALENDAR;
    }
}

package lsfusion.interop.form.object.table.grid;

public enum ListViewType {
    GRID, PIVOT, MAP;

    // should correspond ListViewType in System.lsf
    public String getObjectName() {
        switch (this) {
            case GRID:
                return "grid";
            case PIVOT:
                return "pivot";
            case MAP:
                return "map";
        }
        throw new UnsupportedOperationException();
    }
}

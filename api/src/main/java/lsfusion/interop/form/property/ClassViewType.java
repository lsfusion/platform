package lsfusion.interop.form.property;

public enum ClassViewType {
    PANEL, TOOLBAR, GRID, PIVOT, MAP;

    public static ClassViewType DEFAULT = GRID;
    
    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
    }

    public boolean isToolbar() {
        return this == TOOLBAR;
    }

    public boolean isGrid() {
        return this == GRID;
    }

    public boolean isPivot() {
        return this == PIVOT;
    }

    public boolean isMap() {
        return this == MAP;
    }

    public boolean isList() {
        return isGrid() || isPivot() || isMap();
    }
}

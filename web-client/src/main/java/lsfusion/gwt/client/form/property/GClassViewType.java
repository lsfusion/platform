package lsfusion.gwt.client.form.property;

public enum GClassViewType {
    PANEL, TOOLBAR, GRID, PIVOT, MAP;

    public static final GClassViewType DEFAULT = GRID;
    public boolean isGrid() {
        return this == GRID;
    }

    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
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

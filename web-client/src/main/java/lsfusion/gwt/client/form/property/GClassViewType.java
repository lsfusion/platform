package lsfusion.gwt.client.form.property;

public enum GClassViewType {
    PANEL, TOOLBAR, GRID, HIDE;

    public static final GClassViewType DEFAULT = GRID;
    public boolean isGrid() {
        return this == GRID;
    }

    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
    }

    public boolean isList() {
        return isGrid();
    }
}

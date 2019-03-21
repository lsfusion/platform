package lsfusion.gwt.shared.form.property;

public enum GClassViewType {
    PANEL, TOOLBAR, GRID, HIDE;

    public static final GClassViewType DEFAULT = GRID;
    public boolean isGrid() {
        return this == GRID;
    }

    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
    }

    public boolean isHidden() {
        return this == HIDE;
    }
}

package lsfusion.interop.form.property;

public enum ClassViewType {
    PANEL, TOOLBAR, GRID;

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

    public boolean isList() {
        return isGrid();
    }
}

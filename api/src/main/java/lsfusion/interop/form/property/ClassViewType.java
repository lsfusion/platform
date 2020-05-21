package lsfusion.interop.form.property;

public enum ClassViewType {
    PANEL, TOOLBAR, LIST;

    public static ClassViewType DEFAULT = LIST;
    
    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
    }

    public boolean isToolbar() {
        return this == TOOLBAR;
    }

    public boolean isList() {
        return this == LIST;
    }
}

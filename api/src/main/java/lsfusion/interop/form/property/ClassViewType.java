package lsfusion.interop.form.property;

public enum ClassViewType {
    PANEL, TOOLBAR, POPUP, LIST;

    public static ClassViewType DEFAULT = LIST;
    
    // a view in which a group shows its current object alone, as in a panel: all but a table's
    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR || this == POPUP;
    }

    public boolean isToolbar() {
        return this == TOOLBAR;
    }

    public boolean isPopup() {
        return this == POPUP;
    }

    public boolean isList() {
        return this == LIST;
    }
}

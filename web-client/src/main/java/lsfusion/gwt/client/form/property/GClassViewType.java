package lsfusion.gwt.client.form.property;

// the server's view types (ClassViewType) under the same names: a group's view type comes over by its name
public enum GClassViewType {
    PANEL, TOOLBAR, POPUP, LIST;

    public static final GClassViewType DEFAULT = LIST;

    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR || this == POPUP;
    }

    public boolean isList() {
        return this == LIST;
    }
}

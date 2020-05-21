package lsfusion.gwt.client.form.property;

public enum GClassViewType {
    PANEL, TOOLBAR, LIST;

    public static final GClassViewType DEFAULT = LIST;

    public boolean isPanel() {
        return this == PANEL || this == TOOLBAR;
    }

    public boolean isList() {
        return this == LIST;
    }
}

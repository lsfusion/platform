package platform.gwt.form.shared.view;

public enum GContainerType {
    CONTAINERH,
    CONTAINERV,
    CONTAINERVH,
    VERTICAL_SPLIT_PANEL,
    HORIZONTAL_SPLIT_PANEL,
    TABBED_PANEL;

    public boolean isSplit() {
        return this == HORIZONTAL_SPLIT_PANEL || this == VERTICAL_SPLIT_PANEL;
    }

    public boolean isTabbed() {
        return this == TABBED_PANEL;
    }

    public boolean isContainer() {
        return !isSplit() && !isTabbed();
    }
}

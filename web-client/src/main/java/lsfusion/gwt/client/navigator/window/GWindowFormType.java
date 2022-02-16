package lsfusion.gwt.client.navigator.window;

public enum GWindowFormType {
    FLOAT, DOCKED, EMBEDDED, POPUP;

    public boolean isEmbedded() {
        return this == EMBEDDED;
    }

    public boolean isPopup() {
        return this == POPUP;
    }

    public boolean isEditing() {
        return isEmbedded() || isPopup();
    }
}

package lsfusion.gwt.client.navigator.window;

public enum GModalityWindowFormType implements GWindowFormType {

    FLOAT, DOCKED, EMBEDDED, POPUP;

    @Override
    public boolean isFloat() {
        return this == FLOAT;
    }

    @Override
    public boolean isDocked() {
        return this == DOCKED;
    }

    @Override
    public boolean isEmbedded() {
        return this == EMBEDDED;
    }

    @Override
    public boolean isPopup() {
        return this == POPUP;
    }

    @Override
    public boolean isEditing() {
        return isEmbedded() || isPopup();
    }
}
package lsfusion.gwt.client.navigator.window;

public enum GModalityType {
    DOCKED, DOCKED_MODAL, MODAL, DIALOG_MODAL;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isModalWindow() {
        return this == MODAL || this == DIALOG_MODAL;
    }

    public boolean isDialog() {
        return this == DIALOG_MODAL;
    }
}

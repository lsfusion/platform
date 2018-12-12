package lsfusion.gwt.shared.view.window;

public enum GModalityType {
    DOCKED, MODAL, DOCKED_MODAL, DIALOG_MODAL;

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

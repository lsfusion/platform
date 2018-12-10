package lsfusion.gwt.form.shared.view.window;

public enum GModalityType {
    DOCKED, MODAL, DOCKED_MODAL, FULLSCREEN_MODAL, DIALOG_MODAL;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isFullScreen() {
        return this == FULLSCREEN_MODAL;
    }

    public boolean isModalWindow() {
        return this == MODAL || this == FULLSCREEN_MODAL || this == DIALOG_MODAL;
    }

    public boolean isDialog() {
        return this == DIALOG_MODAL;
    }
}

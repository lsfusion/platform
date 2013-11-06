package lsfusion.interop;

public enum ModalityType {
    DOCKED, MODAL, FULLSCREEN_MODAL, DOCKED_MODAL, DIALOG_MODAL;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isDialog() {
        return this == DIALOG_MODAL;
    }

    public boolean isFullScreen() {
        return this == FULLSCREEN_MODAL;
    }
}

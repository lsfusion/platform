package platform.interop;

public enum ModalityType {
    DOCKED, MODAL, FULLSCREEN_MODAL, DOCKED_MODAL;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isFullScreen() {
        return this == FULLSCREEN_MODAL;
    }
}

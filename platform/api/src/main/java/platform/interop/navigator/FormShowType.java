package platform.interop.navigator;

public enum FormShowType {
    DOCKING, MODAL, MODAL_FULLSCREEN;

    public boolean isModal() {
        return this == MODAL || this == MODAL_FULLSCREEN;
    }

    public boolean isFullScreen() {
        return this == MODAL_FULLSCREEN;
    }
}

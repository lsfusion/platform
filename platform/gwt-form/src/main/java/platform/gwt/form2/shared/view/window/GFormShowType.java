package platform.gwt.form2.shared.view.window;

public enum GFormShowType {
    DOCKING, MODAL, MODAL_FULLSCREEN;

    public boolean isModal() {
        return this == MODAL || this == MODAL_FULLSCREEN;
    }

    public boolean isFullScreen() {
        return this == MODAL_FULLSCREEN;
    }
}

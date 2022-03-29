package lsfusion.interop.form;

// временно так, потом возможно будет смысл отдельно разделить на DOCKED / FLOAT, NOWAIT / WAIT, and refactor DIALOG_MODAL to separate parameter
public enum ModalityType {
    DOCKED, MODAL, DOCKED_MODAL, DIALOG_MODAL, EMBEDDED, POPUP;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isWindow() {
        return this == MODAL || this == DIALOG_MODAL;
    }

    public boolean isDialog() {
        return this == DIALOG_MODAL || this == EMBEDDED || this == POPUP;
    }

    public WindowFormType getWindowType() {
        if(this == EMBEDDED)
            return WindowFormType.EMBEDDED;
        if(this == POPUP)
            return WindowFormType.POPUP;

        if(isWindow())
            return WindowFormType.FLOAT;
        else
            return WindowFormType.DOCKED;
    }
}

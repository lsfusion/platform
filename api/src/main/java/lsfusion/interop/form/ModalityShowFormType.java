package lsfusion.interop.form;

public enum ModalityShowFormType implements ShowFormType {

    MODAL, DIALOG_MODAL, EMBEDDED, POPUP;

    @Override
    public boolean isModal() {
        return true; // DOCKED was the one kind that was not, and docked is DockedShowFormType now
    }

    @Override
    public boolean isWindow() {
        return this == MODAL || this == DIALOG_MODAL;
    }

    @Override
    public boolean isDialog() {
        return this == DIALOG_MODAL || this == EMBEDDED || this == POPUP;
    }

    @Override
    public WindowFormType getWindowType() {
        if (this == EMBEDDED) return ModalityWindowFormType.EMBEDDED;
        if (this == POPUP) return ModalityWindowFormType.POPUP;
        return ModalityWindowFormType.FLOAT; // MODAL and DIALOG_MODAL, the only two left
    }
}
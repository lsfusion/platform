package lsfusion.interop.form;

public enum ModalityShowFormType implements ShowFormType {

    DOCKED, DOCKED_MODAL, MODAL, DIALOG_MODAL, EMBEDDED, POPUP;

    @Override
    public boolean isDockedModal() {
        return this == DOCKED_MODAL;
    }

    @Override
    public boolean isModal() {
        return this != DOCKED;
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
        if (this == EMBEDDED) return WindowFormType.EMBEDDED;
        if (this == POPUP) return WindowFormType.POPUP;

        if (isWindow()) return WindowFormType.FLOAT;
        else return WindowFormType.DOCKED;
    }

    @Override
    public String getName() {
        return name();
    }
}
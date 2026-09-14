package lsfusion.gwt.client.navigator.window;

public enum GModalityShowFormType implements GShowFormType {

    MODAL, DIALOG_MODAL, EMBEDDED, POPUP;

    @Override
    public boolean isModal() {
        return true; // DOCKED was the one kind that was not, and docked is GDockedShowFormType now
    }

    @Override
    public boolean isDialog() {
        return this == DIALOG_MODAL || this == EMBEDDED || this == POPUP;
    }

    @Override
    public boolean isWindow() {
        return this == MODAL || this == DIALOG_MODAL;
    }

    @Override
    public GWindowFormType getWindowType() {
        if(this == EMBEDDED)
            return GModalityWindowFormType.EMBEDDED;
        if(this == POPUP)
            return GModalityWindowFormType.POPUP;

        return GModalityWindowFormType.FLOAT; // MODAL and DIALOG_MODAL, the only two left
    }
}

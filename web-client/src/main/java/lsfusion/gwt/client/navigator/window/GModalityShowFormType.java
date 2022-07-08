package lsfusion.gwt.client.navigator.window;

public enum GModalityShowFormType implements GShowFormType {

    DOCKED, DOCKED_MODAL, MODAL, DIALOG_MODAL, EMBEDDED, POPUP;

    @Override
    public boolean isDocked() {
        return this == DOCKED;
    }

    @Override
    public boolean isDockedModal() {
        return this == DOCKED_MODAL;
    }

    @Override
    public boolean isModal() {
        return this != DOCKED;
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
            return GWindowFormType.EMBEDDED;
        if(this == POPUP)
            return GWindowFormType.POPUP;

        if(isWindow())
            return GWindowFormType.FLOAT;
        else
            return GWindowFormType.DOCKED;
    }
}

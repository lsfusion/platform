package lsfusion.gwt.client.navigator.window;

public enum GModalityType {
    DOCKED, DOCKED_MODAL, MODAL, DIALOG_MODAL, EMBEDDED;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isWindow() {
        return this == MODAL || this == DIALOG_MODAL;
    }

    public boolean isDialog() {
        return this == DIALOG_MODAL || this == EMBEDDED;
    }

    public GWindowFormType getWindowType() {
        if(this == EMBEDDED)
            return GWindowFormType.EMBEDDED;

        if(isWindow())
            return GWindowFormType.FLOAT;
        else
            return GWindowFormType.DOCKED;
    }
}

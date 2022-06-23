package lsfusion.gwt.client.form.controller;

public enum EditMode {
    DEFAULT, LINK, DIALOG;

    public static EditMode getMode(int mode) {
        switch (mode) {
            case 0:
            default:
                return DEFAULT;
            case 1:
                return LINK;
            case 2:
                return DIALOG;
        }
    }

    public int getIndex() {
        switch (this) {
            case DEFAULT:
            default:
                return 0;
            case LINK:
                return 1;
            case DIALOG:
                return 2;
        }
    }
}
package lsfusion.gwt.client.form.controller;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.StaticImage;

public enum EditMode {
    DEFAULT, LINK, DIALOG, GROUPCHANGE;

    public static EditMode getMode(int mode) {
        switch (mode) {
            case 0:
            default:
                return DEFAULT;
            case 1:
                return LINK;
            case 2:
                return DIALOG;
            case 3:
                return GROUPCHANGE;
        }
    }

    public static StaticImage getImage(int mode) {
        switch (mode) {
            case 0:
            default:
                return StaticImage.DEFAULTMODE;
            case 1:
                return StaticImage.LINKMODE;
            case 2:
                return StaticImage.DIALOGMODE;
            case 3:
                return StaticImage.GROUPCHANGEMODE;
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
            case GROUPCHANGE:
                return 3;
        }
    }

    public String getTitle(ClientMessages messages) {
        switch (this) {
            case DEFAULT:
            default:
                return messages.editModeDefault();
            case LINK:
                return messages.editModeLink();
            case DIALOG:
                return messages.editModeDialog();
            case GROUPCHANGE:
                return messages.editModeGroupChange();
        }
    }

}
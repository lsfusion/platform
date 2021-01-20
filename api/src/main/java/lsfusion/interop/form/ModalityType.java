package lsfusion.interop.form;

// временно так, потом возможно будет смысл отдельно разделить на DOCKED / FLOAT, NOWAIT / WAIT, and refactor DIALOG_MODAL to separate parameter
public enum ModalityType {
    DOCKED, MODAL, DOCKED_MODAL, DIALOG_MODAL;

    public static ModalityType deserialize(byte data) {
        switch (data) {
            case 0:
                return DOCKED;
            case 1:
                return MODAL;
            case 2:
                return DOCKED_MODAL;
            case 3:
                return DIALOG_MODAL;
        }
        throw new RuntimeException("Deserialize ModalityType");
    }

    public byte serialize() {
        switch(this) {
            case DOCKED:
                return 0;
            case MODAL:
                return 1;
            case DOCKED_MODAL:
                return 2;
            case DIALOG_MODAL:
                return 3;
        }
        throw new RuntimeException("Serialize ModalityType");
    }

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isModalWindow() {
        return this == MODAL || this == DIALOG_MODAL;
    }

    public boolean isDialog() {
        return this == DIALOG_MODAL;
    }
}

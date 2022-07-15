package lsfusion.interop.form;

import java.io.DataOutputStream;
import java.io.IOException;

public enum ModalityWindowFormType implements WindowFormType {

    FLOAT, DOCKED, EMBEDDED, POPUP;

    @Override
    public boolean isModal() {
        return this != DOCKED;
    }

    @Override
    public boolean isEditing() {
        return this == EMBEDDED || this == POPUP;
    }

    @Override
    public byte getType() {
        switch (this) {
            case FLOAT:
                return 1;
            case DOCKED:
                return 2;
            case EMBEDDED:
                return 3;
            case POPUP:
                return 4;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        serializeType(outStream);
    }

    public static ModalityWindowFormType deserialize(int type) throws IOException {
        switch (type) {
            case 1:
                return FLOAT;
            case 2:
                return DOCKED;
            case 3:
                return EMBEDDED;
            case 4:
                return POPUP;
        }
        throw new UnsupportedOperationException();
    }
}
package lsfusion.interop.form;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public enum WindowFormType {
    FLOAT, DOCKED, EMBEDDED, POPUP;

    public boolean isModal() {
        return this != DOCKED;
    }

    public boolean isEditing() {
        return this == EMBEDDED || this == POPUP;
    }

    public static WindowFormType deserialize(DataInputStream inStream) throws IOException {
        switch(inStream.readByte()) {
            case 0:
                return FLOAT;
            case 1:
                return DOCKED;
            case 2:
                return EMBEDDED;
            case 3:
                return POPUP;
        }
        throw new UnsupportedOperationException();
    }

    public byte getType() {
        switch(this) {
            case FLOAT:
                return 0;
            case DOCKED:
                return 1;
            case EMBEDDED:
                return 2;
            case POPUP:
                return 3;
        }
        throw new UnsupportedOperationException();
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
    }
}

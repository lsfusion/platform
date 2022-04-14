package lsfusion.interop.form.event;

import java.io.DataInputStream;
import java.io.IOException;

public enum BindingMode {
    AUTO, ALL, ONLY, NO, INPUT;

    public int serialize() {
        switch (this) {
            case AUTO:
                return 0;
            case ALL:
                return 1;
            case ONLY:
                return 2;
            case NO:
                return 3;
            case INPUT:
                return 4;
        }
        throw new UnsupportedOperationException();
    }
    public static BindingMode deserialize(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();
        switch (type) {
            case 0:
                return BindingMode.AUTO;
            case 1:
                return BindingMode.ALL;
            case 2:
                return BindingMode.ONLY;
            case 3:
                return BindingMode.NO;
            case 4:
                return BindingMode.INPUT;
        }
        throw new UnsupportedOperationException();
    }
}
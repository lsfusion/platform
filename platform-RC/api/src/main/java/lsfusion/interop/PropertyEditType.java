package lsfusion.interop;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public enum PropertyEditType {
    EDITABLE, READONLY, SELECTOR;

    public static PropertyEditType deserialize(byte data) throws IOException {
        switch(data) {
            case 0:
                return EDITABLE;
            case 1:
                return READONLY;
            case 2:
                return SELECTOR;
        }
        throw new RuntimeException("Deserialize PropertyEditType");
    }

    public byte serialize() throws IOException {
        switch(this) {
            case EDITABLE:
                return 0;
            case READONLY:
                return 1;
            case SELECTOR:
                return 2;
        }
        throw new RuntimeException("Serialize PropertyEditType");
    }

    public static PropertyEditType getReadonlyType(boolean readonly) {
        return readonly ? READONLY : EDITABLE;
    }

    public static List<String> typeNameList() {
        return Arrays.asList("EDITABLE", "READONLY", "SELECTOR");
    }
}

package lsfusion.interop.form.property;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public enum PropertyEditType {
    EDITABLE, READONLY;
    
    public static PropertyEditType deserialize(byte data) {
        switch(data) {
            case 0:
                return EDITABLE;
            case 1:
                return READONLY;
        }
        throw new RuntimeException("Deserialize PropertyEditType");
    }

    public byte serialize() {
        switch(this) {
            case EDITABLE:
                return 0;
            case READONLY:
                return 1;
        }
        throw new RuntimeException("Serialize PropertyEditType");
    }

    public static PropertyEditType getReadonlyType(boolean readonly) {
        return readonly ? READONLY : EDITABLE;
    }

    public static List<String> typeNameList() {
        return Arrays.asList("EDITABLE", "READONLY");
    }
}

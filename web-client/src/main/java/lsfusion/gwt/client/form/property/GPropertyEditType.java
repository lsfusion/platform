package lsfusion.gwt.client.form.property;

import java.io.IOException;

public enum GPropertyEditType {
    EDITABLE, READONLY, DISABLE;

    public static GPropertyEditType deserialize(byte data) throws IOException {
        switch(data) {
            case 0:
                return EDITABLE;
            case 1:
                return READONLY;
            case 2:
                return DISABLE;
        }
        throw new RuntimeException("Deserialize GPropertyEditType");
    }

    public byte serialize() throws IOException {
        switch(this) {
            case EDITABLE:
                return 0;
            case READONLY:
                return 1;
            case DISABLE:
                return 2;
        }
        throw new RuntimeException("Serialize GPropertyEditType");
    }
}

package platform.gwt.form.shared.view;

import java.io.IOException;

public enum GPropertyEditType {
    EDITABLE, READONLY, SELECTOR;

    public static GPropertyEditType deserialize(byte data) throws IOException {
        switch(data) {
            case 0:
                return EDITABLE;
            case 1:
                return READONLY;
            case 2:
                return SELECTOR;
        }
        throw new RuntimeException("Deserialize GPropertyEditType");
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
        throw new RuntimeException("Serialize GPropertyEditType");
    }
}

package lsfusion.interop.form.property;

public enum PropertyEditType {
    EDITABLE, READONLY, DISABLE;
    
    public static PropertyEditType deserialize(byte data) {
        switch(data) {
            case 0:
                return EDITABLE;
            case 1:
                return READONLY;
            case 2:
                return DISABLE;
        }
        throw new RuntimeException("Deserialize PropertyEditType");
    }

    public byte serialize() {
        switch(this) {
            case EDITABLE:
                return 0;
            case READONLY:
                return 1;
            case DISABLE:
                return 2;
        }
        throw new RuntimeException("Serialize PropertyEditType");
    }
}

package lsfusion.server.data.table;

public enum IndexType {
    DEFAULT, LIKE, MATCH;

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public static IndexType deserialize(byte data) {
        switch (data) {
            case 0:
                return DEFAULT;
            case 1:
                return LIKE;
            case 2:
                return MATCH;
        }
        throw new RuntimeException("Deserialize IndexType");
    }

    public byte serialize() {
        switch (this) {
            case DEFAULT:
                return 0;
            case LIKE:
                return 1;
            case MATCH:
                return 2;
        }
        throw new RuntimeException("Serialize IndexType");
    }

}
package lsfusion.server.data.table;

import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;

public enum IndexType {
    DEFAULT, LIKE, MATCH;

    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isLike() {
        return this == LIKE;
    }

    public boolean isMatch() {
        return this == MATCH;
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
    
    public String suffix() {
        switch (this) {
            case DEFAULT: return "_default";
            case LIKE: return "_like";
            case MATCH: return "_match";
        }
        assert(false);
        return "";
    }

    private static final AddValue<Object, IndexType> addValue = new SymmAddValue<Object, IndexType>() {
        @Override
        public IndexType addValue(Object key, IndexType prevValue, IndexType newValue) {
            if(prevValue.isMatch() || newValue.isMatch())
                return MATCH;
            if(prevValue.isLike() || newValue.isLike())
                return LIKE;
            return DEFAULT;
        }
    };
    public static <K> AddValue<K, IndexType> addValue() {
        return (AddValue<K, IndexType>) addValue;
    }
}
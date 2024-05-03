package lsfusion.interop.form.property;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.ApiResourceBundle.getString;

public enum Compare {
    EQUALS("="), GREATER(">"), LESS("<"), GREATER_EQUALS(">="), 
    LESS_EQUALS("<="), NOT_EQUALS("!="), CONTAINS("_"), MATCH("@"), 
    INARRAY("IN ARRAY");

    private final String str;

    private static final Map<String, Compare> lookup = new HashMap<>();

    static {
        for (Compare c : Compare.values()) {
            lookup.put(c.str, c);
        }
    }

    Compare(String str) {
        this.str = str;
    }

    public static Compare get(String str) {
        return lookup.get(str);
    }

    public static Compare get(boolean min) {
        return min?Compare.LESS:Compare.GREATER;
    }

    public static Compare deserialize(DataInputStream inStream) throws IOException {
        return deserialize(inStream.readByte());
    }

    public static Compare deserialize(byte ibyte) throws IOException {
        switch(ibyte) {
            case -1:
                return null;
            case 0:
                return EQUALS;
            case 1:
                return GREATER;
            case 2:
                return LESS;
            case 3:
                return GREATER_EQUALS;
            case 4:
                return LESS_EQUALS;
            case 5:
                return NOT_EQUALS;
            case 6:
                return CONTAINS;
            case 7:
                return MATCH;
        }
        throw new RuntimeException("Deserialize Compare");
    }

    public byte serialize() {
        switch(this) {
            case EQUALS:
                return 0;
            case GREATER:
                return 1;
            case LESS:
                return 2;
            case GREATER_EQUALS:
                return 3;
            case LESS_EQUALS:
                return 4;
            case NOT_EQUALS:
                return 5;
            case CONTAINS:
                return 6;
            case MATCH:
                return 7;
        }
        throw new RuntimeException("Serialize Compare");
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(serialize());
    }

    public Compare reverse() {
        switch(this) {
            case EQUALS:
                return EQUALS;
            case GREATER:
                return LESS;
            case LESS:
                return GREATER;
            case GREATER_EQUALS:
                return LESS_EQUALS;
            case LESS_EQUALS:
                return GREATER_EQUALS;
            case NOT_EQUALS:
                return NOT_EQUALS;
            case CONTAINS:
                return CONTAINS;
            case MATCH:
                return MATCH;
        }
        throw new RuntimeException("not supported yet");
    }

    @Override
    public String toString() {
        return str;
    }

    public String getFullString() {
        switch (this) {
            case EQUALS :
            case GREATER :
            case LESS :
            case GREATER_EQUALS :
            case LESS_EQUALS :
                return str;
            case NOT_EQUALS :
                return str + " (" + getString("filter.compare.not.equals") + ")";
            case CONTAINS:
                return str + " (" + getString("filter.compare.contains") + ")";
            case MATCH:
                return str + " (" + getString("filter.compare.search") + ")";
        }
        return "";
    }

    public String getTooltipText() {
        switch (this) {
            case EQUALS :
                return getString("filter.compare.equals");
            case GREATER :
                return getString("filter.compare.greater");
            case LESS :
                return getString("filter.compare.less");
            case GREATER_EQUALS :
                return getString("filter.compare.greater.equals");
            case LESS_EQUALS :
                return getString("filter.compare.less.equals");
            case NOT_EQUALS :
                return getString("filter.compare.not.equals");
            case CONTAINS:
                return getString("filter.compare.contains");
            case MATCH:
                return getString("filter.compare.search");
        }
        return "";
    }

    public boolean escapeSeparator() {
        return this == EQUALS || this == CONTAINS || this == MATCH;
    }
}

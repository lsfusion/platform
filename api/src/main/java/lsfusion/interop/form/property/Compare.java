package lsfusion.interop.form.property;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.ApiResourceBundle.getString;

public enum Compare {
    EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, CONTAINS, MATCH, INARRAY;

    public static Compare get(boolean min) {
        return min?Compare.LESS:Compare.GREATER;
    }

    public static Compare deserialize(DataInputStream inStream) throws IOException {
        switch(inStream.readByte()) {
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
            case 8:
                return INARRAY;
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
            case INARRAY:
                return 8;
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
        }
        throw new RuntimeException("not supported yet");
    }

    @Override
    public String toString() {
        switch (this) {
            case EQUALS :
                return "=";
            case GREATER :
                return ">";
            case LESS :
                return "<";
            case GREATER_EQUALS :
                return ">=";
            case LESS_EQUALS :
                return "<=";
            case NOT_EQUALS :
                return "!=";
            case CONTAINS:
                return "_";
            case MATCH:
                return "@";
            case INARRAY :
                return "IN ARRAY";
        }
        throw new RuntimeException("Serialize Compare");
    }

    public String getFullString() {
        switch (this) {
            case EQUALS :
                return "=";
            case GREATER :
                return ">";
            case LESS :
                return "<";
            case GREATER_EQUALS :
                return ">=";
            case LESS_EQUALS :
                return "<=";
            case NOT_EQUALS :
                return "!= (" + getString("filter.compare.not.equals") + ")";
            case CONTAINS:
                return "_ (" + getString("filter.compare.contains") + ")";
            case MATCH:
                return "@ (" + getString("filter.compare.search") + ")";
            case INARRAY :
                return getString("filter.compare.in.array");
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
            case INARRAY :
                return getString("filter.compare.in.array");
        }
        return "";
    }

    //todo: rename to escapeSeparator
    public boolean escapeComma() {
        return this == EQUALS || this == CONTAINS || this == MATCH;
    }
}

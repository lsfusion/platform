package lsfusion.interop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.base.ApiResourceBundle.getString;

public enum Compare {
    EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, START_WITH, CONTAINS, ENDS_WITH, LIKE, INARRAY;

    public static Compare get(boolean min) {
        return min?Compare.LESS:Compare.GREATER;
    }

    public static Compare deserialize(DataInputStream inStream) throws IOException {
        switch(inStream.readByte()) {
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
                return START_WITH;
            case 7:
                return CONTAINS;
            case 8:
                return ENDS_WITH;
            case 9:
                return LIKE;
            case 10:
                return INARRAY;
        }
        throw new RuntimeException("Deserialize Compare");
    }

    public byte serialize() throws IOException {
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
            case START_WITH:
                return 6;
            case CONTAINS:
                return 7;
            case ENDS_WITH:
                return 8;
            case LIKE:
                return 9;
            case INARRAY:
                return 10;
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
            case START_WITH :
                return getString("interop.starts.with");
            case CONTAINS:
                return getString("interop.contains");
            case ENDS_WITH :
                return getString("interop.ends.with");
            case LIKE :
                return "LIKE";
            case INARRAY :
                return "IN ARRAY";
        }
        throw new RuntimeException("Serialize Compare");
    }
}

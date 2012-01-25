package platform.interop;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static platform.base.ApiResourceBundle.getString;

public enum Compare {
    EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, START_WITH, LIKE;

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
                return LIKE;
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
            case LIKE:
                return 7;
        }
        throw new RuntimeException("Serialize Compare");
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(serialize());
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
            case LIKE :
                return "LIKE";
        }
        throw new RuntimeException("Serialize Compare");
    }
}

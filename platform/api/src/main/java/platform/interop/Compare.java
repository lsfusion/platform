package platform.interop;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.DataOutputStream;

public enum Compare {

    EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS;

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
        }
        throw new RuntimeException("Deserialize Compare");
    }
}

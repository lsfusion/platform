package lsfusion.interop;

import java.io.IOException;

public enum Order {
    REPLACE, ADD, REMOVE, DIR;

    public static Order deserialize(byte data) throws IOException {
        switch(data) {
            case 0:
                return REPLACE;
            case 1:
                return ADD;
            case 2:
                return REMOVE;
            case 3:
                return DIR;
        }
        throw new RuntimeException("Deserialize Scroll");
    }

    public byte serialize() throws IOException {
        switch(this) {
            case REPLACE:
                return 0;
            case ADD:
                return 1;
            case REMOVE:
                return 2;
            case DIR:
                return 3;
        }
        throw new RuntimeException("Serialize Scroll");
    }

}

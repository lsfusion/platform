package platform.server.data;

import platform.server.data.type.Type;

import java.io.DataInputStream;
import java.io.IOException;

public class KeyField extends Field implements Comparable<KeyField> {
    public KeyField(String iName, Type iType) {super(iName,iType);}

    public KeyField(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    byte getType() {
        return 0;
    }

    public int compareTo(KeyField o) {
        return name.compareTo(o.name); 
    }
}

package platform.server.data;

import platform.server.data.types.Type;

import java.io.DataInputStream;
import java.io.IOException;

public class KeyField extends Field {
    public KeyField(String iName, Type iType) {super(iName,iType);}

    public KeyField(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    byte getType() {
        return 0;
    }
}

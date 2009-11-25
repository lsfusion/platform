package platform.server.data;

import platform.server.data.type.Type;

import java.io.DataInputStream;
import java.io.IOException;

public class PropertyField extends Field {

    public PropertyField(String iName, Type iType) {
        super(iName,iType);
    }

    public PropertyField(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    byte getType() {
        return 1;
    }
}

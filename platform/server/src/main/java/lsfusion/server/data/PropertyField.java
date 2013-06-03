package lsfusion.server.data;

import lsfusion.server.data.type.Type;

import java.io.DataInputStream;
import java.io.IOException;

public class PropertyField extends Field {

    public PropertyField(String name, Type type) {
        super(name,type);
    }

    public PropertyField(DataInputStream inStream, int version) throws IOException {
        super(inStream, version);
    }

    byte getType() {
        return 1;
    }
}

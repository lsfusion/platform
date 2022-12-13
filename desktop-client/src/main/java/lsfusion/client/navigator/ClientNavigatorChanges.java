package lsfusion.client.navigator;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.base.BaseUtils.deserializeObject;
import static lsfusion.base.BaseUtils.deserializeString;

public class ClientNavigatorChanges {

    public Map<ClientPropertyNavigator, Object> properties;

    public ClientNavigatorChanges(byte[] formChanges) throws IOException {
        this(new DataInputStream(new ByteArrayInputStream(formChanges)));
    }

    public ClientNavigatorChanges(DataInputStream inStream) throws IOException {
        properties = new HashMap<>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            properties.put(deserializePropertyNavigator(inStream), deserializeObject(inStream));
        }
    }

    public ClientPropertyNavigator deserializePropertyNavigator(DataInputStream inStream) throws IOException {
        byte type = inStream.readByte();
        switch (type) {
            case 0:
                String canonicalName = deserializeString(inStream);
                return new ClientCaptionPropertyNavigator(canonicalName);
            default:
                throw new UnsupportedOperationException("Unsupported ClientPropertyNavigator");
        }
    }

}
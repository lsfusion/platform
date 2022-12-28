package lsfusion.client.navigator;

import lsfusion.base.file.IOUtils;

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
            byte type = inStream.readByte();
            ClientPropertyNavigator propertyNavigator;
            Object value;
            switch (type) {
                case 0:
                    String canonicalName = deserializeString(inStream);
                    propertyNavigator = new ClientCaptionElementNavigator(canonicalName);
                    value = deserializeObject(inStream);
                    break;
                case 1:
                    canonicalName = deserializeString(inStream);
                    propertyNavigator = new ClientImageElementNavigator(canonicalName);
                    boolean staticImage = inStream.readBoolean();
                    value = staticImage ? IOUtils.readImageIcon(inStream) : deserializeObject(inStream);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported ClientPropertyNavigator");
            }
            properties.put(propertyNavigator, value);
        }
    }
}
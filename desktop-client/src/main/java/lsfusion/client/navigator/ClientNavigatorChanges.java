package lsfusion.client.navigator;

import lsfusion.base.file.IOUtils;
import lsfusion.client.navigator.window.ClientClassWindowNavigator;

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
            String canonicalName = deserializeString(inStream);
            switch (type) {
                case 0:
                    propertyNavigator = new ClientCaptionElementNavigator(canonicalName);
                    break;
                case 1:
                    propertyNavigator = new ClientImageElementNavigator(canonicalName);
                    break;
                case 2:
                    propertyNavigator = new ClientClassElementNavigator(canonicalName);
                    break;
                case 10:
                    propertyNavigator = new ClientClassWindowNavigator(canonicalName);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported ClientPropertyNavigator");
            }
            properties.put(propertyNavigator, deserializeObject(inStream));
        }
    }
}
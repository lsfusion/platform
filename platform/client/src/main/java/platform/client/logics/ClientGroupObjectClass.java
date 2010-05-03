package platform.client.logics;

import platform.base.OrderedMap;
import platform.client.logics.classes.ClientClass;

import java.io.Serializable;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientGroupObjectClass extends OrderedMap<ClientObjectImplementView,ClientClass>
                             implements Serializable {

    public ClientGroupObjectClass(DataInputStream inStream,ClientGroupObjectImplementView clientGroupObject, boolean nulls) throws IOException {
        for (ClientObjectImplementView clientObject : clientGroupObject) {
            put(clientObject, (nulls && inStream.readBoolean()) ? null : ClientClass.deserialize(inStream));
        }
    }

}

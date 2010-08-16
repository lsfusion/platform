package platform.client.logics;

import platform.base.OrderedMap;
import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientGroupObjectClass extends OrderedMap<ClientObject,ClientClass>
                             implements Serializable {

    public ClientGroupObjectClass(DataInputStream inStream, ClientGroupObject clientGroupObject, boolean nulls) throws IOException {
        for (ClientObject clientObject : clientGroupObject) {
            put(clientObject, (nulls && inStream.readBoolean()) ? null : ClientClass.deserialize(inStream));
        }
    }

}

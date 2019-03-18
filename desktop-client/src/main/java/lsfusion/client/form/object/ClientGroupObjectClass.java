package lsfusion.client.form.object;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.client.classes.ClientClass;
import lsfusion.client.classes.ClientTypeSerializer;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

public class ClientGroupObjectClass extends OrderedMap<ClientObject,ClientClass>
                             implements Serializable {

    public ClientGroupObjectClass(DataInputStream inStream, ClientGroupObject clientGroupObject, boolean nulls) throws IOException {
        for (ClientObject clientObject : clientGroupObject.objects) {
            put(clientObject, (nulls && inStream.readBoolean()) ? null : ClientTypeSerializer.deserializeClientClass(inStream));
        }
    }

}

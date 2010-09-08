package platform.client.logics;

import static platform.base.BaseUtils.*;
import platform.base.OrderedMap;

import java.io.*;
import java.util.Map;

public class ClientGroupObjectValue extends OrderedMap<ClientObject,Object>
                             implements Serializable {
    public ClientGroupObjectValue(ClientGroupObjectValue... clones) {
        super();
        for (ClientGroupObjectValue clone : clones) {
            putAll(clone);
        }
    }

    public ClientGroupObjectValue(DataInputStream inStream, ClientPropertyDraw clientPropertyDraw) throws IOException {
        for (ClientObject clientObject : clientPropertyDraw.groupObject) {
            put(clientObject, deserializeObject(inStream));
        }

        for (ClientGroupObject columnGroupObject : clientPropertyDraw.columnGroupObjects) {
            for (ClientObject clientObject : columnGroupObject) {
                put(clientObject, deserializeObject(inStream));
            }
        }
    }
    
    public ClientGroupObjectValue(DataInputStream inStream, ClientGroupObject clientGroupObject) throws IOException {
        for (ClientObject clientObject : clientGroupObject) {
            put(clientObject, deserializeObject(inStream));
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for (Map.Entry<ClientObject,Object> objectValue : entrySet()) {
            serializeObject(outStream, objectValue.getValue());
        }
    }

    public boolean contains(ClientGroupObjectValue other) {
        for (Map.Entry<ClientObject, Object> entry : other.entrySet()) {
            Object key = entry.getKey();
            if (!this.containsKey(key)) {
                return false;
            }

            if (!nullEquals(entry.getValue(), this.get(key))) {
                return false;
            }
        }

        return true;
    }
}

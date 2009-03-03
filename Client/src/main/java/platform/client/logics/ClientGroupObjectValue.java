package platform.client.logics;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class ClientGroupObjectValue extends ClientGroupObjectMap<Integer>
                             implements Serializable {

    public ClientGroupObjectValue(DataInputStream inStream,ClientGroupObjectImplementView clientGroupObject) throws IOException {
        for (ClientObjectImplementView clientObject : clientGroupObject) {
            put(clientObject, inStream.readInt());
        }
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for (Map.Entry<ClientObjectImplementView,Integer> objectValue : entrySet()) {
            outStream.writeInt(objectValue.getValue());
        }        
    }
}

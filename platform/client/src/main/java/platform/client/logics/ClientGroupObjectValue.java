package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.OrderedMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class ClientGroupObjectValue extends OrderedMap<ClientObjectImplementView,Integer>
                             implements Serializable {

    public ClientGroupObjectValue(DataInputStream inStream,ClientGroupObjectImplementView clientGroupObject) throws IOException {
        for (ClientObjectImplementView clientObject : clientGroupObject)
            put(clientObject, inStream.readInt());
    }
    
    public ClientGroupObjectValue(DataInputStream inStream,ClientGroupObjectImplementView clientGroupObject,boolean nulls) throws IOException {
        for (ClientObjectImplementView clientObject : clientGroupObject)
            put(clientObject, (Integer) BaseUtils.deserializeObject(inStream));

    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for (Map.Entry<ClientObjectImplementView,Integer> objectValue : entrySet())
            outStream.writeInt(objectValue.getValue());
    }
}

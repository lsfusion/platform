package platform.client.logics;

import platform.base.OrderedMap;
import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class ClientGroupObjectValue extends OrderedMap<ClientObjectImplementView,Object>
                             implements Serializable {

    public ClientGroupObjectValue(DataInputStream inStream,ClientGroupObjectImplementView clientGroupObject) throws IOException {
        for (ClientObjectImplementView clientObject : clientGroupObject)
            put(clientObject, BaseUtils.deserializeObject(inStream));
    }
    
    public ClientGroupObjectValue(DataInputStream inStream,ClientGroupObjectImplementView clientGroupObject,boolean nulls) throws IOException {
        for (ClientObjectImplementView clientObject : clientGroupObject)
            put(clientObject, BaseUtils.deserializeObject(inStream));

    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for (Map.Entry<ClientObjectImplementView,Object> objectValue : entrySet())
            BaseUtils.serializeObject(outStream, objectValue.getValue());
    }
}

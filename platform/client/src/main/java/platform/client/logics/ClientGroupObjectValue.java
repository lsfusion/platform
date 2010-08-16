package platform.client.logics;

import platform.base.BaseUtils;
import platform.base.OrderedMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public class ClientGroupObjectValue extends OrderedMap<ClientObject,Object>
                             implements Serializable {

    public ClientGroupObjectValue(DataInputStream inStream, ClientGroupObject clientGroupObject) throws IOException {
        for (ClientObject clientObject : clientGroupObject)
            put(clientObject, BaseUtils.deserializeObject(inStream));
    }
    
    public ClientGroupObjectValue(DataInputStream inStream, ClientGroupObject clientGroupObject,boolean nulls) throws IOException {
        for (ClientObject clientObject : clientGroupObject)
            put(clientObject, BaseUtils.deserializeObject(inStream));

    }

    public void serialize(DataOutputStream outStream) throws IOException {
        for (Map.Entry<ClientObject,Object> objectValue : entrySet())
            BaseUtils.serializeObject(outStream, objectValue.getValue());
    }
}

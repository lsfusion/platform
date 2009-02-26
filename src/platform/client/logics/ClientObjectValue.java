package platform.client.logics;

import platform.base.BaseUtils;
import platform.client.logics.classes.ClientClass;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientObjectValue {

    public ClientClass cls;
    public Object object;

    public ClientObjectValue(DataInputStream inStream) throws IOException {
        cls = ClientClass.deserialize(inStream);
        object = BaseUtils.deserializeObject(inStream);
    }

    public ClientObjectValue(ClientClass icls, Object iobject) {
        cls = icls;
        object = iobject;
    }
}

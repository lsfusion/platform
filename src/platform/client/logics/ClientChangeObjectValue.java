package platform.client.logics;

import platform.base.BaseUtils;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientChangeObjectValue extends ClientChangeValue {
    Object value;

    public ClientChangeObjectValue(DataInputStream inStream) throws IOException {
        super(inStream);
        value = BaseUtils.deserializeObject(inStream);
    }

    public ClientObjectValue getObjectValue(Object ivalue) {
        return new ClientObjectValue(cls, value);
    }
}

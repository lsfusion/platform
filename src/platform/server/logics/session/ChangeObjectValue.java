package platform.server.logics.session;

import platform.server.logics.classes.RemoteClass;
import platform.base.BaseUtils;

import java.io.DataOutputStream;
import java.io.IOException;

public class ChangeObjectValue extends ChangeValue {
    public Object value;

    public ChangeObjectValue(RemoteClass iClass, Object iValue) {
        super(iClass);
        value = iValue;
    }

    byte getTypeID() {
        return 0;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        BaseUtils.serializeObject(outStream,value);
    }
}

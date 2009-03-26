package platform.server.session;

import platform.server.logics.classes.RemoteClass;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract class ChangeValue {
    public RemoteClass changeClass;

    ChangeValue(RemoteClass iClass) {
        changeClass = iClass;
    }

    abstract byte getTypeID();
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeID());
        changeClass.serialize(outStream);
    }
}

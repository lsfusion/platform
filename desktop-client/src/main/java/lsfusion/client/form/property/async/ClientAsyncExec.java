package lsfusion.client.form.property.async;

import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class ClientAsyncExec extends ClientAsyncEventExec {

    public static ClientAsyncExec deserialize(DataInputStream inStream) throws IOException {
        int asyncType = inStream.readInt();
        if (asyncType == 0)
            return new ClientAsyncOpenForm(SerializationUtil.readString(inStream), inStream.readBoolean());
        else return null;
    }

    public abstract void exec();
}

package lsfusion.client.form.property.async;

import lsfusion.interop.form.remote.serialization.SerializationUtil;

import java.io.DataInputStream;
import java.io.IOException;

public abstract class ClientAsyncExec extends ClientAsyncEventExec {

    public abstract void exec(long requestIndex);

    public ClientAsyncExec() {
    }

    public ClientAsyncExec(DataInputStream inStream) {
        super(inStream);
    }
}

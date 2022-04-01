package lsfusion.client.form.property.async;

import java.io.DataInputStream;

public abstract class ClientAsyncFormExec extends ClientAsyncEventExec {

    public ClientAsyncFormExec() {
    }

    public ClientAsyncFormExec(DataInputStream inStream) {
        super(inStream);
    }
}

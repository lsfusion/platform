package lsfusion.client.form.property.async;

import java.io.DataInputStream;

public abstract class ClientAsyncInputExec extends ClientAsyncEventExec {

    public ClientAsyncInputExec() {
    }

    public ClientAsyncInputExec(DataInputStream inStream) {
        super(inStream);
    }
}

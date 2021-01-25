package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientAsyncOpenForm implements ClientAsyncExec {
    public String caption;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncOpenForm() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        this.caption = pool.readString(inStream);
        this.modal = pool.readBoolean(inStream);
    }
}

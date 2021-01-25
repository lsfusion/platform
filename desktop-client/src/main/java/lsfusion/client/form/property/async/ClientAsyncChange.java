package lsfusion.client.form.property.async;

import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientAsyncChange implements ClientAsyncExec {
    public ClientType changeType;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncChange() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        this.changeType = ClientTypeSerializer.deserializeClientType(inStream);
    }
}

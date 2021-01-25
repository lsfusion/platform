package lsfusion.client.form.property.async;

import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientAsyncAddRemove implements ClientAsyncExec {
    public ClientObject object;
    public Boolean add;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncAddRemove() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        this.object = pool.deserializeObject(inStream);
        this.add = inStream.readBoolean();
    }
}

package lsfusion.client.form.property;

import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientPivotColumn implements ClientPropertyDrawOrPivotColumn {
    public String groupObject;

    @SuppressWarnings("unused")
    public ClientPivotColumn() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) {
        //unused
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        groupObject = pool.readString(inStream);
    }
}

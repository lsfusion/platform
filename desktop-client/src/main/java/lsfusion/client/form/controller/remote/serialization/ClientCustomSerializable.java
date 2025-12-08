package lsfusion.client.form.controller.remote.serialization;

import lsfusion.interop.form.remote.serialization.CustomSerializable;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientCustomSerializable extends CustomSerializable<ClientSerializationPool> {

    default void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException();
    }
}

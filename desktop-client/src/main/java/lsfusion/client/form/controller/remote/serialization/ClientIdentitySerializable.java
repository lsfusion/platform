package lsfusion.client.form.controller.remote.serialization;

import lsfusion.interop.form.remote.serialization.IdentitySerializable;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientIdentitySerializable extends ClientCustomSerializable, IdentitySerializable<ClientSerializationPool> {

    default void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException();
    }
}

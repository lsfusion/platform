package lsfusion.client.form.controller.remote.serialization;

import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientIdentitySerializable extends ClientCustomSerializable {

    void setID(int ID);

    default void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException();
    }
}

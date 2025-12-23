package lsfusion.client.form.controller.remote.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ClientCustomSerializable {

    void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException;
}

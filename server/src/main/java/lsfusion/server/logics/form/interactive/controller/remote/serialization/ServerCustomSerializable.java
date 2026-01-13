package lsfusion.server.logics.form.interactive.controller.remote.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface ServerCustomSerializable {

    void customSerialize(ServerSerializationPool pool, DataOutputStream outStream) throws IOException;
}

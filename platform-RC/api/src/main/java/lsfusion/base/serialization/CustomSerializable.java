package lsfusion.base.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface CustomSerializable<P extends SerializationPool> {
    void customSerialize(P pool, DataOutputStream outStream, String serializationType) throws IOException;
    void customDeserialize(P pool, DataInputStream inStream) throws IOException;
}

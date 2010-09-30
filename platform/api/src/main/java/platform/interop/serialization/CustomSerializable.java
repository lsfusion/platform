package platform.interop.serialization;

import java.io.DataOutputStream;
import java.io.IOException;

public interface CustomSerializable {
    public void customSerialize(SerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException;
}

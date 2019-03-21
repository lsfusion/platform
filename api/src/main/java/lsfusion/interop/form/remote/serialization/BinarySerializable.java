package lsfusion.interop.form.remote.serialization;

import java.io.DataOutputStream;
import java.io.IOException;

public interface BinarySerializable {
    void write(DataOutputStream out) throws IOException;
}

package lsfusion.base;

import lsfusion.base.col.interfaces.immutable.ImList;

import java.io.DataOutputStream;
import java.io.IOException;

public interface BinarySerializable {
    void write(DataOutputStream out) throws IOException;
}

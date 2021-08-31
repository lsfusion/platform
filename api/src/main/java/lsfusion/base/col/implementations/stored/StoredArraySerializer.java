package lsfusion.base.col.implementations.stored;

import java.io.*;

public interface StoredArraySerializer {
    byte[] serialize(Object o);
    Object deserialize(int id, byte[] buf);
    
    int getId(Object o);
    
    static byte[] serializeSerializable(Object o) throws IOException {
        assert o instanceof Serializable;

        try (
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream ostream = new ObjectOutputStream(bytes);
        ) {
            ostream.writeObject(o);
            return bytes.toByteArray();
        }
    }

    static Object deserializeSerializable(byte[] buf) throws IOException, ClassNotFoundException {
        try (
            ByteArrayInputStream bytes = new ByteArrayInputStream(buf);
            ObjectInputStream istream = new ObjectInputStream(bytes);
        ) {
            Object obj = istream.readObject();
            return obj;
        }
    }
    
}

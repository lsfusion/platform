package lsfusion.base.col.implementations.stored;

import java.io.*;

public interface StoredArraySerializer {
    void serialize(Object o, ByteArrayOutputStream oStream);
    Object deserialize(ByteArrayInputStream buf);
    
    void setContext(Object context);
    Object getContext();    
    
    boolean canBeSerialized(Object o);
    
    static void serializeSerializable(Object o, ByteArrayOutputStream outStream) throws IOException {
        assert o instanceof Serializable;
        try (ObjectOutputStream objStream = new ObjectOutputStream(outStream)) {
            objStream.writeObject(o);
        }
    }

    static Object deserializeSerializable(ByteArrayInputStream inpStream) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objStream = new ObjectInputStream(inpStream)) {
            return objStream.readObject();
        }
    }
    
    static StoredArraySerializer getInstance() {
        return StoredArraySerializerRegistry.getInstance();
    } 
}

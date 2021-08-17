package lsfusion.base.col.implementations.stored;

import java.util.function.Function;

public interface StoredArraySerializer {
    int register(Class<?> cls, Function<Object, byte[]> serializeFunc, Function<byte[], Object> deserializeFunc);
    
    byte[] serialize(Object o);
    Object deserialize(int id, byte[] buf);
    
    int getId(Object o);
}

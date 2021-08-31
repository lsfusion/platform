package lsfusion.base.col.implementations.stored;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StoredArraySerializerRegistry implements StoredArraySerializer {
    private final Map<Class<?>, Integer> idMap = new HashMap<>();
    private final Map<Integer, Function<Object, byte[]>> serializeMap = new HashMap<>();
    private final Map<Integer, Function<byte[], Object>> deserializeMap = new HashMap<>();
    
    public int register(Class<?> cls, Function<Object, byte[]> serializeFunc, Function<byte[], Object> deserializeFunc) {
        idMap.putIfAbsent(cls, idMap.size()+1); // id is 1-based, 0 is for Serializable interface 
        int id = idMap.get(cls);
        serializeMap.put(id, serializeFunc);
        deserializeMap.put(id, deserializeFunc);
        return id;
    }

    @Override
    public byte[] serialize(Object o) {
        int id = getId(o);
        if (id != 0) {
            return serializeMap.get(id).apply(o);
        } else {
            assert o instanceof Serializable;
            try {
                return StoredArraySerializer.serializeSerializable(o);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public Object deserialize(int id, byte[] buf) {
        if (id != 0) {
            assert deserializeMap.containsKey(id);
            return deserializeMap.get(id).apply(buf);
        } else {
            try {
                return StoredArraySerializer.deserializeSerializable(buf);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int getId(Object o) {
        Integer id = getId(o.getClass());
        if (id != null) {
            return id;
        } else {
            assert o instanceof Serializable;
            return 0;
        }
    }
    
    private Integer getId(Class<?> cls) {
        return idMap.get(cls);    
    }
}

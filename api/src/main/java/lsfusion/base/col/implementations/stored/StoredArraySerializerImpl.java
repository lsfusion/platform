package lsfusion.base.col.implementations.stored;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StoredArraySerializerImpl implements StoredArraySerializer {
    private final Map<Class<?>, Integer> idMap = new HashMap<>();
    private final Map<Integer, Function<Object, byte[]>> serializeMap = new HashMap<>();
    private final Map<Integer, Function<byte[], Object>> deserializeMap = new HashMap<>();
    
    @Override
    public int register(Class<?> cls, Function<Object, byte[]> serializeFunc, Function<byte[], Object> deserializeFunc) {
        idMap.putIfAbsent(cls, idMap.size()+1);
        int id = idMap.get(cls);
        serializeMap.put(id, serializeFunc);
        deserializeMap.put(id, deserializeFunc);
        return id;
    }

    @Override
    public byte[] serialize(Object o) {
        Integer id = idMap.get(o.getClass());
        assert id != null;
        return serializeMap.get(id).apply(o);
    }

    @Override
    public Object deserialize(int id, byte[] buf) {
        assert deserializeMap.containsKey(id);
        return deserializeMap.get(id).apply(buf);
    }

    @Override
    public int getId(Object o) {
        return getId(o.getClass());
    }
    
    private int getId(Class<?> cls) {
        return idMap.get(cls);    
    }
}

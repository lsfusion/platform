package lsfusion.base.col.implementations.stored;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class StoredArraySerializerRegistry implements StoredArraySerializer {
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }    
    
    private final Map<Class<?>, Integer> idMap = new HashMap<>();
    private final Map<Integer, TriConsumer<Object, StoredArraySerializer, ByteArrayOutputStream>> serializeMap = new HashMap<>();
    private final Map<Integer, BiFunction<ByteArrayInputStream, StoredArraySerializer, Object>> deserializeMap = new HashMap<>();
    
    public int register(Class<?> cls, TriConsumer<Object, StoredArraySerializer, ByteArrayOutputStream> serializeFunc, 
                                      BiFunction<ByteArrayInputStream, StoredArraySerializer, Object> deserializeFunc) {
        idMap.putIfAbsent(cls, idMap.size()+1); // id is 1-based, 0 is for Serializable interface 
        int id = idMap.get(cls);
        serializeMap.put(id, serializeFunc);
        deserializeMap.put(id, deserializeFunc);
        return id;
    }

    @Override
    public void serialize(Object o, ByteArrayOutputStream oStream) {
        int id = getId(o);
        try {
            try (DataOutputStream dataStream = new DataOutputStream(oStream)) {
                dataStream.writeShort(id);
            }
            if (id != 0) {
                serializeMap.get(id).accept(o, this, oStream);
            } else {
                assert o instanceof Serializable;
                StoredArraySerializer.serializeSerializable(o, oStream);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);  
        }
    }

    @Override
    public Object deserialize(ByteArrayInputStream iStream) {
        DataInputStream dataStream = new DataInputStream(iStream);

        int id;
        try {
            id = dataStream.readShort();
            if (id != 0) {
                assert deserializeMap.containsKey(id);
                return deserializeMap.get(id).apply(iStream, this);
            } else {
                return StoredArraySerializer.deserializeSerializable(iStream);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
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

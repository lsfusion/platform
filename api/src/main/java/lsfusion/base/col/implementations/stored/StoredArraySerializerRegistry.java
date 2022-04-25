package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.order.*;
import lsfusion.base.col.implementations.simple.*;

import java.io.*;
import java.lang.ref.WeakReference;
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
    
    private final int NULL_ID = 0;
    private final int SERIALIZABLE_ID = 1; 
    
    public int register(Class<?> cls, TriConsumer<Object, StoredArraySerializer, ByteArrayOutputStream> serializeFunc, 
                                      BiFunction<ByteArrayInputStream, StoredArraySerializer, Object> deserializeFunc) {
        idMap.putIfAbsent(cls, idMap.size()+2); // id is 2-based, 0 is for null, 1 is for Serializable interface 
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
            if (id == SERIALIZABLE_ID) {
                assert o instanceof Serializable;
                StoredArraySerializer.serializeSerializable(o, oStream);
            } else if (id != NULL_ID) {
                serializeMap.get(id).accept(o, this, oStream);
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
            if (id == NULL_ID) {
                return null;
            } else if (id == SERIALIZABLE_ID) {
                return StoredArraySerializer.deserializeSerializable(iStream);
            } else {
                assert deserializeMap.containsKey(id);
                return deserializeMap.get(id).apply(iStream, this);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private int getId(Object o) {
        if (o == null) {
            return NULL_ID;
        } 
        Integer id = getId(o.getClass());
        if (id != null) {
            return id;
        } else if (o instanceof Serializable) {
            return SERIALIZABLE_ID;  
        }
        throw new StoredArraySerializerException(String.format("Serialization of '%s' object is not supported", o.getClass()));
    }

    public static class StoredArraySerializerException extends RuntimeException {
        public StoredArraySerializerException(String message) {
            super(message);
        }
    }

    private Integer getId(Class<?> cls) {
        return idMap.get(cls);    
    }

    private final ThreadLocal<WeakReference<Object>> context = new ThreadLocal<>();
    
    @Override
    public void setContext(Object context) {
        this.context.set(new WeakReference<>(context));
    }
    
    @Override
    public Object getContext() {
        if (context.get() == null) {
            return null;
        }
        return context.get().get();
    }

    @Override
    public boolean canBeSerialized(Object o) {
        return o == null || getId(o.getClass()) != null || o instanceof Serializable;
    }

    public static StoredArraySerializerRegistry getInstance() {
        if (instance == null) {
            synchronized (StoredArraySerializerRegistry.class) {
                if (instance == null) {
                    instance = new StoredArraySerializerRegistry();

                    instance.register(EmptySet.class, (o, s, outS) -> {}, (inS, s) -> EmptySet.INSTANCE());
                    instance.register(EmptyOrderSet.class, (o, s, outS) -> {}, (inS, s) -> EmptyOrderSet.INSTANCE());
                    instance.register(EmptyOrderMap.class, (o, s, outS) -> {}, (inS, s) -> EmptyOrderMap.INSTANCE());
                    instance.register(EmptyRevMap.class, (o, s, outS) -> {}, (inS, s) -> EmptyRevMap.INSTANCE());

                    instance.register(SingletonRevMap.class, SingletonRevMap::serialize, SingletonRevMap::deserialize);
                    instance.register(SingletonOrderMap.class, SingletonOrderMap::serialize, SingletonOrderMap::deserialize);
                    instance.register(SingletonSet.class, SingletonSet::serialize, SingletonSet::deserialize);
                    instance.register(SingletonOrderSet.class, SingletonOrderSet::serialize, SingletonOrderSet::deserialize);

                    instance.register(ArCol.class, ArCol::serialize, ArCol::deserialize);
                    instance.register(ArList.class, ArList::serialize, ArList::deserialize);
                    instance.register(ArSet.class, ArSet::serialize, ArSet::deserialize);
                    instance.register(HSet.class, HSet::serialize, HSet::deserialize);
                    instance.register(ArIndexedSet.class, ArIndexedSet::serialize, ArIndexedSet::deserialize);

                    instance.register(ArOrderSet.class, ArOrderSet::serialize, ArOrderSet::deserialize);
                    instance.register(ArOrderIndexedSet.class, ArOrderIndexedSet::serialize, ArOrderIndexedSet::deserialize);
                    instance.register(HOrderSet.class, HOrderSet::serialize, HOrderSet::deserialize);

                    instance.register(ArMap.class, ArMap::serialize, ArMap::deserialize);
                    instance.register(HMap.class, HMap::serialize, HMap::deserialize);
                    instance.register(ArIndexedMap.class, ArIndexedMap::serialize, ArIndexedMap::deserialize);

                    instance.register(ArOrderMap.class, ArOrderMap::serialize, ArOrderMap::deserialize);
                    instance.register(ArOrderIndexedMap.class, ArOrderIndexedMap::serialize, ArOrderIndexedMap::deserialize);
                    instance.register(HOrderMap.class, HOrderMap::serialize, HOrderMap::deserialize);
                }
            }
        }
        return instance;
    }

    private static StoredArraySerializerRegistry instance = null;
}

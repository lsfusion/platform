package lsfusion.base.serialization;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.context.ApplicationContext;
import lsfusion.base.context.ApplicationContextHolder;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import static lsfusion.base.ApiResourceBundle.getString;

public abstract class SerializationPool<C> {
    private Map<Integer, Class<? extends CustomSerializable<? extends SerializationPool<C>>>> idToClass
            = new HashMap<Integer, Class<? extends CustomSerializable<? extends SerializationPool<C>>>>();
    private Map<Class<? extends CustomSerializable<? extends SerializationPool<C>>>, Integer> classToId
            = new HashMap<Class<? extends CustomSerializable<? extends SerializationPool<C>>>, Integer>();

    private Map<Long, CustomSerializable> objects = new HashMap<Long, CustomSerializable>();

    public C context;
    public ApplicationContext appContext;
    private static final int NULL_REF_CLASS = -1;

    public SerializationPool() {
        this(null);
    }

    public SerializationPool(C context) {
        this(context, null);
    }

    public SerializationPool(C context, ApplicationContext appContext) {
        this.context = context;
        this.appContext = appContext;
    }

    protected void addMapping(Class<? extends CustomSerializable<? extends SerializationPool<C>>> clazz) {
        int classId = idToClass.size();

        idToClass.put(classId, clazz);
        classToId.put(clazz, classId);
    }

    public CustomSerializable get(int classId, int id) {
        return objects.get(getLongId(classId, id));
    }

    public void put(int classId, int id, CustomSerializable value) {
        objects.put(getLongId(classId, id), value);
    }

    private long getLongId(int classId, int id) {
        return (((long) classId) << 32L) | id;
    }

    public int getClassId(Class clazz) {
        //делаем поиск вверх по иерархии...
        while (clazz != null && !classToId.containsKey(clazz)) {
            clazz = clazz.getSuperclass();
        }

        if (!classToId.containsKey(clazz)) {
            throw new IllegalArgumentException(getString("serialization.unknown.class", clazz));
        }

        return classToId.get(clazz);
    }

    public Class<? extends CustomSerializable<? extends SerializationPool<C>>> getClassById(int classId) {
        if (!idToClass.containsKey(classId)) {
            throw new IllegalArgumentException(getString("serialization.unknown.class.identifier.serialization", classId));
        }
        return idToClass.get(classId);
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> List<T> deserializeList(DataInputStream inStream) throws IOException {
        return (List<T>) deserializeCollection(new ArrayList<T>(), inStream);
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> Set<T> deserializeSet(DataInputStream inStream) throws IOException {
        return (Set<T>) deserializeCollection(new HashSet<T>(), inStream);
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> Collection<T> deserializeCollection(Collection<T> collection, DataInputStream inStream) throws IOException {
        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            T element = (T) deserializeObject(inStream);
            collection.add(element);
        }
        return collection;
    }

    public <K extends CustomSerializable<? extends SerializationPool<C>>,
            V extends CustomSerializable<? extends SerializationPool<C>>> Map<K, V> deserializeMap(DataInputStream inStream) throws IOException {
        HashMap<K, V> result = new HashMap<K, V>();

        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            K key = (K) deserializeObject(inStream);
            V value = (V) deserializeObject(inStream);
            result.put(key, value);
        }

        return result;
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> T deserializeObject(DataInputStream inStream) throws IOException {
        int classId = inStream.readInt();
        if (classId == NULL_REF_CLASS) {
            return null;
        }

        Class<? extends CustomSerializable> clazz = getClassById(classId);

        CustomSerializable instance;
        if (IdentitySerializable.class.isAssignableFrom(clazz)) {
            int id = inStream.readInt();
            instance = get(classId, id);
            if (instance == null) {
                instance = createNewInstance(inStream, clazz, id);
            }
        } else {
            instance = createNewInstance(inStream, clazz, -1);
        }

        return (T) instance;
    }

    public <T extends CustomSerializable> void serializeObject(DataOutputStream outStream, T object, String type) throws IOException {
        if (object == null) {
            outStream.writeInt(NULL_REF_CLASS);
            return;
        }

        int classId = getClassId(object.getClass());
        outStream.writeInt(classId);

        if (object instanceof IdentitySerializable) {
            IdentitySerializable identityObj = (IdentitySerializable) object;
            int id = identityObj.getID();
            outStream.writeInt(id);

            if (get(classId, id) == null) {
                put(classId, id, object);
                object.customSerialize(this, outStream, type);
            }
        } else {
            object.customSerialize(this, outStream, type);
        }
    }

    private CustomSerializable<? extends SerializationPool<C>> createNewInstance(DataInputStream inStream, Class<? extends CustomSerializable> clazz, int id) {
        try {
            Constructor<? extends CustomSerializable> ctor = clazz.getConstructor();
            CustomSerializable instance = ctor.newInstance();

            //ложим объект в пул, если надо
            if (IdentitySerializable.class.isAssignableFrom(clazz)) {
                put(getClassId(clazz), id, instance);
                ((IdentitySerializable) instance).setID(id);
            }
            if (setInstanceContext(instance)) {
                appContext.idRegister(id);
            }

            instance.customDeserialize(this, inStream);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(getString("serialization.can.not.create.object.of.class", clazz.toString()), e);
        }
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeCollection(DataOutputStream outStream, Collection<T> list) throws IOException {
        serializeCollection(outStream, list, null);
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeCollection(DataOutputStream outStream, Collection<T> list, String type) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element, type);
        }
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeCollection(DataOutputStream outStream, ImList<T> list) throws IOException {
        serializeCollection(outStream, list, null);
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeCollection(DataOutputStream outStream, ImList<T> list, String type) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element, type);
        }
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeCollection(DataOutputStream outStream, ImSet<T> list) throws IOException {
        serializeCollection(outStream, list, null);
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeCollection(DataOutputStream outStream, ImSet<T> list, String type) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element, type);
        }
    }

    public <K extends CustomSerializable<? extends SerializationPool<C>>,
            V extends CustomSerializable<? extends SerializationPool<C>>> void serializeMap(DataOutputStream outStream, Map<K, V> map) throws IOException {
        serializeMap(outStream, map, null);
    }

    public <K extends CustomSerializable<? extends SerializationPool<C>>,
            V extends CustomSerializable<? extends SerializationPool<C>>> void serializeMap(DataOutputStream outStream, Map<K, V> map, String type) throws IOException {
        outStream.writeInt(map.size());
        for (Map.Entry<K, V> entry : map.entrySet()) {
            serializeObject(outStream, entry.getKey(), type);
            serializeObject(outStream, entry.getValue(), type);
        }
    }

    public <T extends CustomSerializable<? extends SerializationPool<C>>> void serializeObject(DataOutputStream outStream, T object) throws IOException {
        serializeObject(outStream, object, null);
    }

    public void writeObject(DataOutputStream outStream, Object object) throws IOException {
        outStream.writeBoolean(object != null);
        if (object != null) {
            new ObjectOutputStream(outStream).writeObject(object);
        }
    }

    public <T> T readObject(DataInputStream inStream) throws IOException {
        try {
            if (inStream.readBoolean()) {
                T object = (T) new ObjectInputStream(inStream).readObject();
                setInstanceContext(object);
                return object;
            } else {
                return null;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(getString("serialization.can.not.read.object"), e);
        }
    }

    public void writeString(DataOutputStream outStream, String str) throws IOException {
        SerializationUtil.writeString(outStream, str);
    }

    public String readString(DataInputStream inStream) throws IOException {
        return SerializationUtil.readString(inStream);
    }

    public void writeInt(DataOutputStream outStream, Integer integer) throws IOException {
        SerializationUtil.writeInt(outStream, integer);
    }

    public Integer readInt(DataInputStream inStream) throws IOException {
        return SerializationUtil.readInt(inStream);
    }

    public void writeLong(DataOutputStream outStream, Long n) throws IOException {
        SerializationUtil.writeLong(outStream, n);
    }

    public Long readLong(DataInputStream inStream) throws IOException {
        return SerializationUtil.readLong(inStream);
    }

    private boolean setInstanceContext(Object instance) {
        if (instance instanceof ApplicationContextHolder && appContext != null) {
            ((ApplicationContextHolder) instance).setContext(appContext);
            return true;
        }
        return false;
    }
}

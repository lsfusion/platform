package platform.interop.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class SerializationPool {
    private Map<Integer, Class<? extends CustomSerializable>> idToClass = new HashMap<Integer,Class<? extends CustomSerializable>>();
    private Map<Class<? extends CustomSerializable>, Integer> classToId = new HashMap<Class<? extends CustomSerializable>, Integer>();

    private Map<Long, CustomSerializable> objects = new HashMap<Long, CustomSerializable>();

    public Object context;
    private static final int NULL_REF_CLASS = -1;

    public SerializationPool() {
        this(null);
    }
    
    public SerializationPool(Object context) {
        this.context = context;
    }

    protected void addMapping(Class<? extends CustomSerializable> clazz) {
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
        return (((long)classId) << 32L) | id;
    }

    public int getClassId(Class clazz) {
        //делаем поиск вверх по иерархии...
        while (clazz != null && !classToId.containsKey(clazz)) {
            clazz = clazz.getSuperclass();
        }

        if (!classToId.containsKey(clazz)) {
            throw new IllegalArgumentException("Неизвестный класс: " + clazz);
        }

        return classToId.get(clazz);
    }

    public Class<? extends CustomSerializable> getClassById(int classId) {
        if (!idToClass.containsKey(classId)) {
            throw new IllegalArgumentException("Неизвестный идентификатор класса сериализации: " + classId);
        }
        return idToClass.get(classId);
    }

    public <T extends CustomSerializable> List<T> deserializeList(DataInputStream inStream) throws IOException {
        return (List<T>) deserializeCollection(new ArrayList<T>(), inStream);
    }

    public <T extends CustomSerializable> Set<T> deserializeSet(DataInputStream inStream) throws IOException {
        return (Set<T>) deserializeCollection(new HashSet<T>(), inStream);
    }

    public <T extends CustomSerializable> Collection<T> deserializeCollection(Collection<T> collection, DataInputStream inStream) throws IOException {
        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            T element = (T) deserializeObject(inStream);
            collection.add(element);
        }
        return collection;
    }

    public CustomSerializable deserializeObject(DataInputStream inStream) throws IOException {
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

        return instance;
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

    private CustomSerializable createNewInstance(DataInputStream inStream, Class<? extends CustomSerializable> clazz, int id) {
        try {
            Constructor<? extends CustomSerializable> ctor = clazz.getConstructor();
            CustomSerializable instance = ctor.newInstance();

            //ложим объект в пул, если надо
            if (IdentitySerializable.class.isAssignableFrom(clazz)) {
                put(getClassId(clazz), id, instance);
            }

            instance.customDeserialize(this, id, inStream);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Не могу создать объект класса: " + clazz.toString(), e);
        }
    }

    public <T extends CustomSerializable> void serializeCollection(DataOutputStream outStream, Collection<T> list) throws IOException {
        serializeCollection(outStream, list, null);
    }

    public <T extends CustomSerializable> void serializeCollection(DataOutputStream outStream, Collection<T> list, String type) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element, type);
        }
    }

    public <T extends CustomSerializable> void serializeObject(DataOutputStream outStream, T object) throws IOException {
        serializeObject(outStream, object, null);
    }
}

package platform.interop.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class SerializationPool {
    private Map<Integer, Class<? extends CustomSerializable>> idToClass = new HashMap<Integer,Class<? extends CustomSerializable>>();
    private Map<Class<? extends CustomSerializable>, Integer> classToId = new HashMap<Class<? extends CustomSerializable>, Integer>();

    private Map<Integer, CustomSerializable> objects = new HashMap<Integer, CustomSerializable>();

    public Object context;

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

    public CustomSerializable get(int id) {
        return objects.get(id);
    }

    public void put(int id, CustomSerializable value) {
        objects.put(id, value);
    }

    public int getClassId(Class clazz) {
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

    public List<CustomSerializable> deserializeList(DataInputStream inStream) throws IOException {
        return (List<CustomSerializable>) deserializeCollection(new ArrayList<CustomSerializable>(), inStream);
    }

    public Set<CustomSerializable> deserializeSet(DataInputStream inStream) throws IOException {
        return (Set<CustomSerializable>) deserializeCollection(new HashSet<CustomSerializable>(), inStream);
    }

    public Collection<CustomSerializable> deserializeCollection(Collection<CustomSerializable> collection, DataInputStream inStream) throws IOException {
        int size = inStream.readInt();
        for (int i = 0; i < size; ++size) {
            CustomSerializable element = deserializeObject(inStream);
            collection.add(element);
        }
        return collection;
    }

    public CustomSerializable deserializeObject(DataInputStream inStream) throws IOException {
        int classId = inStream.readInt();
        Class<? extends CustomSerializable> clazz = getClassById(classId);

        CustomSerializable instance;
        if (IdentitySerializable.class.isAssignableFrom(clazz)) {
            int id = inStream.readInt();
            instance = get(id);
            if (instance == null) {
                instance = createNewInstance(inStream, clazz, id);
            }
        } else {
            instance = createNewInstance(inStream, clazz, -1);
        }

        return instance;
    }

    public <T extends CustomSerializable> void serializeObject(DataOutputStream outStream, T object, String type) throws IOException {
        int classId = getClassId(object.getClass());
        outStream.writeInt(classId);

        if (object instanceof IdentitySerializable) {
            IdentitySerializable identityObj = (IdentitySerializable) object;
            int id = identityObj.getID();
            outStream.writeInt(id);
            if (get(id) == null) {
                object.customSerialize(this, outStream, type);
                put(id, object);
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
                put(id, instance);
            }

            instance.customDeserialize(this, id, inStream);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Не могу создать объект класса: " + clazz.toString());
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

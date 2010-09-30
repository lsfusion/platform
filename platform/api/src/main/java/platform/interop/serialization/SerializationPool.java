package platform.interop.serialization;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SerializationPool {
    private Map<Integer, Class<? extends CustomSerializable>> idToClass = new HashMap<Integer,Class<? extends CustomSerializable>>();
    private Map<Class<? extends CustomSerializable>, Integer> classToId = new HashMap<Class<? extends CustomSerializable>, Integer>();

    private Map<Integer, Object> objects = new HashMap<Integer, Object>();

    public SerializationPool() {
    }

    protected void addMapping(Class<? extends CustomSerializable> clazz) {
        int classId = idToClass.size();
        
        idToClass.put(classId, clazz);
        classToId.put(clazz, classId);
    }

    public Object get(int id) {
        return objects.get(id);
    }

    public void put(int id, Object value) {
        objects.put(id, value);
    }

    public int getClassId(Class clazz) {
        return classToId.get(clazz);
    }

    public Class getClassName(int classId) {
        return idToClass.get(classId);
    }

    public Object deserialize(DataInputStream inStream) throws IOException {
        int classId = inStream.readInt();

        Class clazz = getClassName(classId);
        if (clazz == null) {
            throw new IllegalArgumentException("Неизвестный идентификатор класса сериализации");
        }

        Object instance;
        if (IdentitySerializable.class.isAssignableFrom(clazz)) {
            int id = inStream.readInt();
            instance = get(id);
            if (instance == null) {
                instance = createNewInstance(inStream, clazz, id);
            }
        } else {
            instance = createNewInstance(inStream, clazz);
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

    private Object createNewInstance(DataInputStream inStream, Class clazz, int id) {
        try {
            //class SampleClass implements IdentitySerializable
            //    Обязательный конструктор
            //    public SampleClass(SerializationPool pool, int id, DataInputStream inStream)
            Constructor ctor = clazz.getConstructor(Integer.class, DataInputStream.class);
            return ctor.newInstance(id, inStream);
        } catch (Exception e) {
            throw new RuntimeException("Не могу создать объект класса: " + clazz.toString());
        }
    }

    private Object createNewInstance(DataInputStream inStream, Class clazz) {
        try {
            //class SampleClass implements CustomSerializable
            //    Обязательный конструктор
            //    public SampleClass(SerializationPool pool, DataInputStream inStream)
            Constructor ctor = clazz.getConstructor(DataInputStream.class);
            return ctor.newInstance(inStream);
        } catch (Exception e) {
            throw new RuntimeException("Не могу создать объект класса: " + clazz.toString());
        }
    }
    
    public <T extends CustomSerializable> void serializeList(DataOutputStream outStream, Collection<T> list) throws IOException {
        serializeList(outStream, null, list);
    }

    public <T extends CustomSerializable> void serializeList(DataOutputStream outStream, String type, Collection<T> list) throws IOException {
        outStream.writeInt(list.size());
        for (T element : list) {
            serializeObject(outStream, element, type);
        }
    }

    public <T extends CustomSerializable> void serializeObject(DataOutputStream outStream, T object) throws IOException {
        serializeObject(outStream, object, null);
    }
}

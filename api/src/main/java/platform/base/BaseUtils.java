package platform.base;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseUtils {

    public static boolean compareObjects(Object obj1, Object obj2) {

        if (obj1 == null)
            return obj2 == null;
        else
            return obj1.equals(obj2);
    }

    public static <T> boolean findByReference(Collection<T> col, Object obj) {
        for (T objCol : col)
            if (objCol == obj) return true;
        return false;
    }

    public static Object multiply(Object obj, Integer coeff) {

        if (obj instanceof Integer) return ((Integer)obj) * coeff;
        if (obj instanceof Long) return ((Long)obj) * coeff;
        if (obj instanceof Double) return ((Double)obj) * coeff;

        return obj;
    }

    public static <KA,VA,KB,VB> boolean mapEquals(Map<KA,VA> mapA,Map<KB,VB> mapB,Map<KA,KB> mapAB) {
        for(Map.Entry<KA,VA> A : mapA.entrySet())
            if(!mapB.get(mapAB.get(A.getKey())).equals(A.getValue()))
                return false;
        return true;
    }

    public static <K,E,V> Map<K,V> join(Map<K,E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet())
            result.put(entry.getKey(),joinMap.get(entry.getValue()));
        return result;
    }

    public static <K,E,V> LinkedHashMap<V,E> linkedJoin(LinkedHashMap<K,E> map, Map<K,V> joinMap) {
        LinkedHashMap<V,E> result = new LinkedHashMap<V, E>();
        for(Map.Entry<K,E> entry : map.entrySet())
            result.put(joinMap.get(entry.getKey()),entry.getValue());
        return result;
    }

    static <K,E,V> Map<K,V> innerJoin(Map<K,E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if(joinValue!=null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    public static <K,V> Map<K,V> filter(Map<K,V> map, Collection<V> values) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,V> entry : map.entrySet())
            if(values.contains(entry.getValue()))
                result.put(entry.getKey(),entry.getValue());
        return result;
    }

    public static <K,V> Map<V,K> reverse(Map<K,V> map) {
        Map<V,K> result = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            result.put(entry.getValue(),entry.getKey());
        return result;
    }

    public static <KA,VA,KB,VB> Map<VA,VB> crossJoin(Map<KA,VA> map,Map<KB,VB> mapTo,Map<KA,KB> mapJoin) {
        return join(join(reverse(map),mapJoin),mapTo);
    }

    public static <K,V> boolean identity(Map<K,V> map) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(!entry.getKey().equals(entry.getValue())) return false;
        return true;
    }

    public static <K> Map<K,K> toMap(Collection<K> collection) {
        Map<K,K> result = new HashMap<K, K>();
        for(K object : collection)
            result.put(object,object);
        return result;
    }


    public static Object deserializeObject(byte[] state) throws IOException {

        return deserializeObject(new DataInputStream(new ByteArrayInputStream(state)));
    }

    public static Object deserializeObject(DataInputStream inStream) throws IOException {

/*        try {
            ObjectInputStream dataStream = new ObjectInputStream(inStream);
            try {
                return dataStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;*/

        int objectType = inStream.readByte();

        if (objectType == 0) {
            return null;
        }

        if (objectType == 1) {
            return inStream.readInt();
        }

        if (objectType == 2) {
            return inStream.readUTF();
        }

        if (objectType == 3) {
            return inStream.readDouble();
        }

        if (objectType == 4) {
            return inStream.readLong();
        }

        if (objectType == 5) {
            return inStream.readBoolean();
        }

        throw new IOException();
    }

    public static void serializeObject(DataOutputStream outStream, Object object) throws IOException {

/*        try {
            ObjectOutputStream objectStream = new ObjectOutputStream(outStream);
            objectStream.writeObject(object);
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        if (object == null) {
            outStream.writeByte(0);
            return;
        }

        if (object instanceof Integer) {
            outStream.writeByte(1);
            outStream.writeInt((Integer)object);
            return;
        }

        if (object instanceof String) {
            outStream.writeByte(2);
            outStream.writeUTF(((String)object).trim());
            return;
        }

        if (object instanceof Double) {
            outStream.writeByte(3);
            outStream.writeDouble((Double)object);
            return;
        }

        if (object instanceof Long) {
            outStream.writeByte(4);
            outStream.writeLong((Long)object);
            return;
        }

        if (object instanceof Boolean) {
            outStream.writeByte(5);
            outStream.writeBoolean((Boolean)object);
            return;
        }

        throw new IOException();
    }// -------------------------------------- Сериализация классов -------------------------------------------- //

    public static byte[] serializeObject(Object value) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serializeObject(new DataOutputStream(outStream), value);
        return outStream.toByteArray();
    }
}

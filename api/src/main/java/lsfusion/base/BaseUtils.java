package lsfusion.base;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.HMap;
import lsfusion.base.col.implementations.HSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MFilterSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.SimpleAddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static lsfusion.base.ApiResourceBundle.getString;

public class BaseUtils {
    public static final Logger systemLogger = Logger.getLogger("SystemLogger");

    public static final String lineSeparator = System.getProperty("line.separator");

    //Длина строки может быть маскимум 65535, каждый символ может занимать от 1 до 3х байт
    //используем пессимистичный вариант, чтобы не заниматься реальным рассчётом длины, т.к. это долго
    private static final int STRING_SERIALIZATION_CHUNK_SIZE = 65535/3;

    public static boolean nullEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        else
            return obj1.equals(obj2);
    }

    public static boolean nullHashEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        else
            return hashEquals(obj1, obj2);
    }

    public static int nullHash(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    public static <T> boolean findByReference(Collection<T> col, Object obj) {
        for (T objCol : col)
            if (objCol == obj) return true;
        return false;
    }

    public static boolean[] convertArray(Boolean[] array) {
        boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++)
            result[i] = array[i];
        return result;
    }

    public static <KA, VA, KB, VB> boolean mapEquals(Map<KA, VA> mapA, Map<KB, VB> mapB, Map<KA, KB> mapAB) {
        for (Map.Entry<KA, VA> A : mapA.entrySet())
            if (!mapB.get(mapAB.get(A.getKey())).equals(A.getValue()))
                return false;
        return true;
    }

    public static <K, E, V> Map<K, V> nullJoin(Map<K, ? extends E> map, Map<E, V> joinMap) {
        return joinMap == null ? null : join(map, joinMap);
    }

    public static <K, E, V> Map<K, V> join(Map<K, ? extends E> map, Map<E, V> joinMap) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, ? extends E> entry : map.entrySet())
            result.put(entry.getKey(), joinMap.get(entry.getValue()));
        return result;
    }

    public static <K, E, V, R extends E> Map<K, V> rightJoin(Map<K, E> map, Map<R, V> joinMap) {
        return BaseUtils.join(BaseUtils.filterValues(map, joinMap.keySet()), joinMap);
    }

    public static <K, VA, VB> Map<VA, VB> rightCrossJoin(Map<K, VA> map, Map<K, VB> joinMap) {
        return BaseUtils.rightJoin(BaseUtils.reverse(map), joinMap);
    }

    public static <K, VA, VB> Map<VA, VB> rightNullCrossJoin(Map<K, VA> map, Map<K, VB> joinMap) {
        return joinMap==null? null : BaseUtils.rightJoin(BaseUtils.reverse(map), joinMap);
    }

    public static <K, E, V> List<Map<K, V>> joinCol(Map<K, ? extends E> map, Collection<Map<E, V>> list) {
        List<Map<K, V>> result = new ArrayList<Map<K, V>>();
        for (Map<E, V> joinMap : list)
            result.add(BaseUtils.join(map, joinMap));
        return result;
    }

    public static <K, V> List<V> mapList(List<? extends K> list, ImMap<K, ? extends V> map) {
        List<V> result = new ArrayList<V>();
        for (K element : list)
            result.add(map.get(element));
        return result;
    }

    public static <K, V> OrderedMap<K, V> mapOrder(List<? extends K> list, Map<K, ? extends V> map) {
        OrderedMap<K, V> result = new OrderedMap<K, V>();
        for (K element : list)
            result.put(element, map.get(element));
        return result;
    }

    public static <K, E, V> OrderedMap<V, E> mapOrder(OrderedMap<K, E> list, Map<K, ? extends V> map) { // map предполагается reversed
        OrderedMap<V, E> result = new OrderedMap<V, E>();
        for (Map.Entry<K, E> entry : list.entrySet())
            result.put(map.get(entry.getKey()), entry.getValue());
        return result;
    }

    public static <K, V> Set<V> mapSet(Set<K> set, Map<K, ? extends V> map) { // map предполагается reversed
        Set<V> result = new HashSet<V>();
        for (K element : set)
            result.add(map.get(element));
        return result;
    }

    public static <K, E, V> Map<K, V> innerJoin(Map<K, ? extends E> map, Map<? extends E, V> joinMap) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, ? extends E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if (joinValue != null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    public static <K, E, V> Map<K, V> nullInnerJoin(Map<K, ? extends E> map, Map<? extends E, V> joinMap) {
        return joinMap==null ? null : innerJoin(map, joinMap);
    }

    public static <K, E, V> OrderedMap<K, V> innerJoin(OrderedMap<K, E> map, Map<E, V> joinMap) {
        OrderedMap<K, V> result = new OrderedMap<K, V>();
        for (Map.Entry<K, E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if (joinValue != null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    public static <K, V, F> Map<K, F> filterValues(Map<K, V> map, Collection<F> values) {
        Map<K, F> result = new HashMap<K, F>();
        for (Map.Entry<K, V> entry : map.entrySet())
            if (values.contains(entry.getValue()))
                result.put(entry.getKey(), (F) entry.getValue());
        return result;
    }

    // необходимо чтобы пересоздавал объект !!! потому как на вход идут mutable'ы
    public static <K, V> Collection<K> filterValues(Map<K, V> map, V value) {
        Collection<K> result = new ArrayList<K>();
        for (Map.Entry<K, V> entry : map.entrySet())
            if (value.equals(entry.getValue()))
                result.add(entry.getKey());
        return result;
    }


    public static <BK, K extends BK, V> Map<K, V> filterKeys(Map<BK, V> map, Iterable<? extends K> keys) {
        Map<K, V> result = new HashMap<K, V>();
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    public static <BK, K extends BK, V> Map<K, V> filterKeys(Iterable<BK> keys, Map<K, V> map) {
        Map<K, V> result = new HashMap<K, V>();
        for (BK key : keys) {
            V value = map.get(key);
            if (value != null) result.put((K) key, value);
        }
        return result;
    }

    public static <BK, K extends BK, V> Map<K, V> filterInclKeys(Map<BK, V> map, Iterable<? extends K> keys) {
        Map<K, V> result = new HashMap<K, V>();
        for (K key : keys) {
            V value = map.get(key);
            assert value!=null;
            result.put(key, value);
        }
        return result;
    }

    // возвращает более конкретный класс если 
    public static <K, V, CV extends V> Map<K, CV> filterClass(Map<K, V> map, Class<CV> cvClass) {
        for (Map.Entry<K, V> entry : map.entrySet())
            if (!cvClass.isInstance(entry.getValue()))
                return new HashMap<K, CV>();
        return (Map<K, CV>) (Map<K, ? extends V>) map;
    }

    public static <K, V> Map<K, V> filterNotKeys(Map<K, V> map, Collection<? extends K> keys) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!keys.contains(entry.getKey()))
                result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V> Map<K, V> filterNotKeys(Map<K, V> map, ImSet<? extends K> keys) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!((ImSet<K>)keys).contains(entry.getKey()))
                result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V> Map<K, V> filterNotValues(Map<K, V> map, Collection<? extends V> values) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!values.contains(entry.getValue()))
                result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <BK,K extends BK> Collection<K> filter(Collection<K> col, Collection<BK> filter) {
        List<K> result = new ArrayList<K>();
        for (K element : col)
            if (filter.contains(element))
                result.add(element);
        return result;
    }

    public static <K> List<K> filterList(List<K> list, ImSet<K> filter) {
        List<K> result = new ArrayList<K>();
        for (K element : list)
            if (filter.contains(element))
                result.add(element);
        return result;
    }

    public static <K> List<K> filterList(List<K> list, Collection<K> filter) {
        List<K> result = new ArrayList<K>();
        for (K element : list)
            if (filter.contains(element))
                result.add(element);
        return result;
    }

    public static <K> List<K> filterNotList(List<K> list, Collection<K> filter) {
        List<K> result = new ArrayList<K>();
        for (K element : list)
            if (!filter.contains(element))
                result.add(element);
        return result;
    }

    public static <K> Set<K> filterSet(Set<K> set, Collection<K> filter) {
        Set<K> result = new HashSet<K>();
        for (K element : filter)
            if (set.contains(element))
                result.add(element);
        return result;
    }

    public static <K> Set<K> filterNotSet(Set<K> set, Collection<K> filter) {
        Set<K> result = new HashSet<K>();
        for (K element : set)
            if (!filter.contains(element))
                result.add(element);
        return result;
    }

    public static <BK,K extends BK> Collection<K> filterNot(Collection<K> col, Collection<BK> filter) {
        List<K> result = new ArrayList<K>();
        for (K element : col)
            if (!filter.contains(element))
                result.add(element);
        return result;
    }

    public static <BK, K extends BK, V> Map<K, V> splitKeys(Map<BK, V> map, HSet<K> keys, Map<BK, V> rest) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<BK, V> entry : map.entrySet())
            if (keys.contains((K) entry.getKey()))
                result.put((K) entry.getKey(), entry.getValue());
            else
                rest.put(entry.getKey(), entry.getValue());
        return result;
    }

    public static <BV, V extends BV, K> Map<K, V> splitValues(Map<K, BV> map, Collection<V> keys, Map<K, BV> rest) {
        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, BV> entry : map.entrySet())
            if (keys.contains(entry.getValue()))
                result.put(entry.getKey(), (V) entry.getValue());
            else
                rest.put(entry.getKey(), entry.getValue());
        return result;
    }

    public static <K, V> Map<K, V> mergeEquals(Map<K, V> full, Map<K, V> part) {
        assert full.keySet().containsAll(part.keySet());

        Map<K, V> result = new HashMap<K, V>();
        for (Map.Entry<K, V> partEntry : part.entrySet())
            if (full.get(partEntry.getKey()).equals(partEntry.getValue()))
                result.put(partEntry.getKey(), partEntry.getValue());
        return result;
    }

    public static <K, V> Map<V, K> reverse(Map<K, V> map) {
        return reverse(map, false);
    }

    public static <K, V> Map<V, K> reverse(Map<K, V> map, boolean ignoreUnique) {
        Map<V, K> result = new HashMap<V, K>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            assert ignoreUnique || !result.containsKey(entry.getValue());
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    public static <K, VA, VB> Map<VA, VB> crossJoin(Map<K, VA> map, Map<K, VB> mapTo) {
        return join(reverse(map), mapTo);
    }

    public static <KA, VA, KB, VB> Map<VA, VB> crossJoin(Map<KA, VA> map, Map<KB, VB> mapTo, Map<KA, KB> mapJoin) {
        return join(crossJoin(map, mapJoin), mapTo);
    }

    public static <KA, KB, V> Map<KA, KB> crossValues(Map<KA, V> map, Map<KB, V> mapTo) {
        return crossValues(map, mapTo, false);
    }

    public static <KA, KB, V> Map<KA, KB> rightCrossValues(Map<KA, V> map, Map<KB, V> mapTo) {
        return rightJoin(map, reverse(mapTo));
    }

    public static <KA, KB, V> Map<KA, KB> crossInnerValues(Map<KA, V> map, Map<KB, V> mapTo) {
        return innerJoin(map, reverse(mapTo));
    }

    public static <KA, KB, V> Map<KA, KB> crossValues(Map<KA, V> map, Map<KB, V> mapTo, boolean ignoreUnique) {
        return join(map, reverse(mapTo, ignoreUnique));
    }

    public static <K, T, VA, VB> Map<T, VA> splitInnerJoin(Map<T, K> mapTo, Map<K, VA> map1, Map<K, VB> map2, Map<T, VB> res2) {
        Map<T, VA> res1 = new HashMap<T, VA>();
        for (Map.Entry<T, K> map : mapTo.entrySet()) {
            VA value1 = map1.get(map.getValue());
            if (value1 != null)
                res1.put(map.getKey(), value1);
            else
                res2.put(map.getKey(), map2.get(map.getValue()));
        }
        return res1;
    }

    public static <K> Collection<K> join(Collection<K> col1, Collection<K> col2) {
        Set<K> result = new HashSet<K>(col1);
        result.addAll(col2);
        return result;
    }

    public static <K, V> boolean identity(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet())
            if (!entry.getKey().equals(entry.getValue())) return false;
        return true;
    }

    public static <K> Map<K, K> toMap(Set<K> collection) {
        Map<K, K> result = new HashMap<K, K>();
        for (K object : collection)
            result.put(object, object);
        return result;
    }

    public static <K> Map<K, K> toMap(Collection<K> collection) {
        Map<K, K> result = new HashMap<K, K>();
        for (K object : collection)
            result.put(object, object);
        return result;
    }

    public static <K, V> Map<K, V> toMap(List<K> from, List<V> to) {
        assert from.size() == to.size();
        Map<K, V> result = new HashMap<K, V>();
        for (int i = 0; i < from.size(); i++)
            result.put(from.get(i), to.get(i));
        return result;
    }

    public static <K> Map<Object, K> toObjectMap(Set<K> collection) {
        Map<Object, K> result = new HashMap<Object, K>();
        for (K object : collection)
            result.put(new Object(), object);
        return result;
    }

    public static <K, V> Map<K, V> toMap(Collection<K> collection, V value) {
        Map<K, V> result = new HashMap<K, V>();
        for (K object : collection)
            result.put(object, value);
        return result;
    }

    public static <K> Map<Integer, K> toMap(List<K> list) {
        Map<Integer, K> result = new HashMap<Integer, K>();
        for (int i = 0; i < list.size(); i++)
            result.put(i, list.get(i));
        return result;
    }

    public static <K, V> OrderedMap<K, V> toOrderedMap(List<? extends K> list, V value) {
        OrderedMap<K, V> result = new OrderedMap<K, V>();
        for (K element : list)
            result.put(element, value);
        return result;
    }

    public static <K> Map<Integer, K> toMap(K[] list) {
        Map<Integer, K> result = new HashMap<Integer, K>();
        for (int i = 0; i < list.length; i++)
            result.put(i, list[i]);
        return result;
    }

    public static <K> List<K> toList(Map<Integer, K> map) {
        List<K> result = new ArrayList<K>();
        for (int i = 0; i < map.size(); i++)
            result.add(map.get(i));
        return result;
    }

    public static Object deserializeObject(byte[] state) throws IOException {
        return deserializeObject(new DataInputStream(new ByteArrayInputStream(state)));
    }

    public static Object deserializeObject(DataInputStream inStream) throws IOException {

        int objectType = inStream.readByte();

        if (objectType == 0) {
            return null;
        }

        if (objectType == 1) {
            return inStream.readInt();
        }

        if (objectType == 2) {
            return deserializeString(inStream);
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

        if (objectType == 6) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.set(inStream.readInt(), inStream.readInt(), inStream.readInt(), 0, 0, 0);
            gc.set(Calendar.MILLISECOND, 0);
            return new java.sql.Date(gc.getTimeInMillis());
        }

        if (objectType == 7) {
            int len = inStream.readInt();
            return IOUtils.readBytesFromStream(inStream, len);
        }

        if (objectType == 8) {
            return new Timestamp(inStream.readLong());
        }

        if (objectType == 9) {
            return new Time(inStream.readLong());
        }

        if (objectType == 10) {
            return new Color(inStream.readInt());
        }

        if (objectType == 11) {
            return readObjectFromStream(inStream);
        }

        throw new IOException();
    }

    public static Object deserializeString(DataInputStream inStream) throws IOException {
        int chunksCount = inStream.readInt();

        if (chunksCount < 0) {
            return null;
        } else if (chunksCount == 0) {
            return "";
        } else {
            StringBuilder result = new StringBuilder((chunksCount - 1) * STRING_SERIALIZATION_CHUNK_SIZE);
            for (int i = 0; i < chunksCount; i++) {
                result.append(inStream.readUTF());
            }

            return result.toString();
        }
    }

    public static Object readObjectFromStream(DataInputStream inStream) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(inStream);
        try {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] serializeObject(Object value) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serializeObject(new DataOutputStream(outStream), value);
        return outStream.toByteArray();
    }

    public static void serializeObject(DataOutputStream outStream, Object object) throws IOException {

        if (object == null) {
            outStream.writeByte(0);
            return;
        }

        if (object instanceof Integer) {
            outStream.writeByte(1);
            outStream.writeInt((Integer) object);
            return;
        }

        if (object instanceof String) {
            outStream.writeByte(2);
            serializeString(outStream, (String) object);
            return;
        }

        if (object instanceof Double) {
            outStream.writeByte(3);
            outStream.writeDouble((Double) object);
            return;
        }

        if (object instanceof Long) {
            outStream.writeByte(4);
            outStream.writeLong((Long) object);
            return;
        }

        if (object instanceof Boolean) {
            outStream.writeByte(5);
            outStream.writeBoolean((Boolean) object);
            return;
        }

        if (object instanceof java.sql.Date) {
            outStream.writeByte(6);
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime((java.sql.Date) object);
            outStream.writeInt(gc.get(Calendar.YEAR));
            outStream.writeInt(gc.get(Calendar.MONTH));
            outStream.writeInt(gc.get(Calendar.DAY_OF_MONTH));
            return;
        }

        if (object instanceof byte[]) {
            byte[] obj = (byte[]) object;
            outStream.writeByte(7);
            outStream.writeInt(obj.length);
            outStream.write(obj);
            return;
        }

        if (object instanceof Timestamp) {
            outStream.writeByte(8);
            outStream.writeLong(((Timestamp) object).getTime());
            return;
        }

        if (object instanceof Time) {
            outStream.writeByte(9);
            outStream.writeLong(((Time) object).getTime());
            return;
        }

        if (object instanceof Color) {
            outStream.writeByte(10);
            outStream.writeInt(((Color) object).getRGB());
            return;
        }

        if (object instanceof BigDecimal) {
            outStream.writeByte(11);
            writeObjectToStream(object, outStream);
            return;
        }

        throw new IOException();
    }// -------------------------------------- Сериализация классов -------------------------------------------- //

    public static void serializeString(DataOutputStream outStream, String str) throws IOException {
        assert str != null;

        if (str == null) {
            outStream.writeInt(-1);
        } else if (str.length() == 0) {
            outStream.writeInt(0);
        } else {
            int chunkSize = STRING_SERIALIZATION_CHUNK_SIZE;
            int length = str.length();
            int chunksCount = (length + chunkSize - 1) / chunkSize;

            outStream.writeInt(chunksCount);

            for (int i = 0; i < chunksCount; i++) {
                outStream.writeUTF(str.substring(i * chunkSize, min(length, (i + 1) * chunkSize)));
            }
        }
    }

    public static void writeObjectToStream(Object object, DataOutputStream outStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outStream);
        oos.writeObject(object);
    }

    public static boolean startsWith(char[] string, int off, char[] check) {
        if (string.length - off < check.length)
            return false;

        for (int i = 0; i < check.length; i++)
            if (string[off + i] != check[i])
                return false;
        return true;
    }

    public static <K, V> Map<K, V> removeKeys(Map<K, V> map, Collection<K> remove) {
        Map<K, V> removeMap = new HashMap<K, V>();
        for (Map.Entry<K, V> property : map.entrySet())
            if (!remove.contains(property.getKey()))
                removeMap.put(property.getKey(), property.getValue());
        return removeMap;
    }

    public static <K, V> Map<K, V> removeKey(Map<K, V> map, K remove) {
        Map<K, V> removeMap = new HashMap<K, V>();
        for (Map.Entry<K, V> property : map.entrySet())
            if (!property.getKey().equals(remove))
                removeMap.put(property.getKey(), property.getValue());
        return removeMap;
    }

    public static <K> Collection<K> add(Collection<? extends K> col, K add) {
        Collection<K> result = new ArrayList<K>(col);
        result.add(add);
        return result;
    }

    public static <K> Set<K> addSet(Set<? extends K> col, K add) {
        Set<K> result = new HashSet<K>(col);
        result.add(add);
        return result;
    }

    public static <K> List<K> add(List<K> col, K add) {
        ArrayList<K> result = new ArrayList<K>(col);
        result.add(add);
        return result;
    }

    public static <K, V> Map<K, V> add(Map<? extends K, ? extends V> map, K add, V addValue) {
        Map<K, V> result = new HashMap<K, V>(map);
        result.put(add, addValue);
        return result;
    }

    public static <K> List<K> add(K add, List<? extends K> col) {
        ArrayList<K> result = new ArrayList<K>();
        result.add(add);
        result.addAll(col);
        return result;
    }

    public static <K> Collection<K> remove(Collection<? extends K> set, Collection<? extends K> remove) {
        Collection<K> result = new ArrayList<K>(set);
        result.removeAll(remove);
        return result;
    }

    public static <K> Collection<K> remove(Collection<? extends K> set, K remove) {
        Collection<K> result = new ArrayList<K>(set);
        result.remove(remove);
        return result;
    }

    public static <K> Set<K> removeSet(Set<? extends K> set, Collection<? extends K> remove) {
        Set<K> result = new HashSet<K>(set);
        result.removeAll(remove);
        return result;
    }

    public static <K> List<K> removeList(List<K> list, Collection<K> remove) {
        List<K> removeList = new ArrayList<K>();
        for (K property : list)
            if (!remove.contains(property))
                removeList.add(property);
        return removeList;
    }

    public static <K> List<K> removeList(List<K> list, K remove) {
        return removeList(list, Collections.singleton(remove));
    }

    public static <K> List<K> removeList(List<K> list, int index) {
        return removeList(list, Collections.singleton(list.get(index)));
    }

    public static <K> K lastSetElement(Set<K> set) {
        K key = null;
        for (K k : set) {
            key = k;
        }
        return key;
    }

    public static <K> void moveElement(List<K> list, K elemFrom, K elemTo) {

        int indFrom = list.indexOf(elemFrom);
        int indTo = list.indexOf(elemTo);

        if (indFrom == -1 || indTo == -1 || indFrom == indTo) return;

        boolean up = indFrom >= indTo;

        list.remove(elemFrom);
        list.add(list.indexOf(elemTo) + (up ? 0 : 1), elemFrom);
    }

    public static <K> void moveElement(List<K> list, K elemFrom, int index) {

        if (index == -1) {
            list.remove(elemFrom);
            list.add(elemFrom);
        } else {
            boolean up = list.indexOf(elemFrom) >= index;

            list.remove(elemFrom);
            list.add(index + (up ? 0 : -1), elemFrom);
        }
    }

    public static <B, K1 extends B, K2 extends B, V> LinkedHashMap<B, V> mergeLinked(LinkedHashMap<K1, ? extends V> map1, LinkedHashMap<K2, ? extends V> map2) {
        LinkedHashMap<B, V> result = new LinkedHashMap<B, V>(map1);
        for (Map.Entry<K2, ? extends V> entry2 : map2.entrySet()) {
            V prevValue = result.put(entry2.getKey(), entry2.getValue());
            assert prevValue == null || prevValue.equals(entry2.getValue());
        }
        return result;
    }

    public static <B, K1 extends B, K2 extends B, V> Map<B, V> merge(Map<K1, ? extends V> map1, Map<K2, ? extends V> map2) {
        Map<B, V> result = new HashMap<B, V>(map1);
        for (Map.Entry<K2, ? extends V> entry2 : map2.entrySet()) {
            V prevValue = result.put(entry2.getKey(), entry2.getValue());
            assert prevValue == null || prevValue.equals(entry2.getValue());
        }
        return result;
    }

    public static <B, K1 extends B, K2 extends B, V> Map<B, V> override(Map<K1, ? extends V> map1, Map<K2, ? extends V> map2) {
        Map<B, V> result = new HashMap<B, V>(map1);
        result.putAll(map2);
        return result;
    }

    public static <B, K1 extends B, K2 extends B, V> Map<K1, V> replace(Map<K1, ? extends V> map1, Map<K2, ? extends V> map2) {
        Map<K1, V> result = new HashMap<K1, V>(map1);
        for (Map.Entry<K1, V> entry : result.entrySet()) {
            V value2 = map2.get(entry.getKey());
            if (value2 != null)
                entry.setValue(value2);
        }
        return result;
    }

    public static <B, K1 extends B, K2 extends B, V> Map<K1, V> replaceValues(Map<K1, ? extends V> map1, Map<? extends V, ? extends V> map2) {
        Map<K1, V> result = new HashMap<K1, V>(map1);
        for (Map.Entry<K1, V> entry : result.entrySet()) {
            V value2 = map2.get(entry.getValue());
            if (value2 != null)
                entry.setValue(value2);
        }
        return result;
    }

    public static <K, V> Map<K, V> replace(Map<K, ? extends V> map, K key, V value) {
        Map<K, V> result = new HashMap<K, V>(map);
        result.put(key, value);
        return result;
    }

    public static <K, V> boolean isSubMap(Map<? extends K, ? extends V> map1, Map<K, ? extends V> map2) {
        for (Map.Entry<? extends K, ? extends V> entry : map1.entrySet()) {
            V value2 = map2.get(entry.getKey());
            if (!(value2 != null && hashEquals(value2, entry.getValue())))
                return false;
        }
        return true;
    }

    public static <B, K1 extends B, K2 extends B> Collection<B> merge(Collection<K1> col1, Collection<K2> col2) {
        Collection<B> result = new ArrayList<B>(col1);
        result.addAll(col2);
        return result;
    }

    public static <B, K1 extends B, K2 extends B> Set<B> mergeSet(Set<K1> set1, Set<K2> set2) {
        Set<B> result = new HashSet<B>(set1);
        result.addAll(set2);
        return result;
    }

    public static <B, K1 extends B, K2 extends B> Set<B> mergeColSet(Collection<K1> set1, Collection<K2> set2) {
        Set<B> result = new HashSet<B>(set1);
        result.addAll(set2);
        return result;
    }

    public static <B, K1 extends B, K2 extends B> Set<B> mergeItem(Set<K1> set, K2 item) {
        Set<B> result = new HashSet<B>(set);
        result.add(item);
        return result;
    }

    public static <B, K1 extends B, K2 extends B> List<B> addList(K1 item, List<K2> list) {
        List<B> result = new ArrayList<B>();
        result.add(item);
        result.addAll(list);
        return result;
    }

    public static <B, K1 extends B, K2 extends B> List<B> mergeList(List<K1> list1, List<K2> list2) {
        List<B> result = new ArrayList<B>(list1);
        result.addAll(list2);
        return result;
    }

    public static <B> List<B> mergeLists(List<B>... lists) {
        List<B> result = new ArrayList<B>();
        for (List<B> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    public static <V, MV, EV> Map<Object, EV> mergeMaps(Map<V, EV> map, Map<MV, EV> toMerge, Map<MV, Object> mergedMap) {
        Map<Object, EV> merged = new HashMap<Object, EV>(map);
        Map<EV, Object> reversed = BaseUtils.reverse(merged);
        for (Map.Entry<MV, EV> transEntry : toMerge.entrySet()) {
            Object mergedProp = reversed.get(transEntry.getValue());
            if (mergedProp == null) {
                mergedProp = new Object();
                merged.put(mergedProp, transEntry.getValue());
            }
            mergedMap.put(transEntry.getKey(), mergedProp);
        }
        return merged;
    }

    // строит декартово произведение нескольких упорядоченных множеств
    public static <T> List<List<T>> cartesianProduct(List<List<T>> data) {
        LinkedList<List<T>> queue = new LinkedList<List<T>>();
        queue.add(new ArrayList<T>());
        final int tupleSize = data.size();
        while (!queue.isEmpty()) {
            if (queue.peekFirst().size() == tupleSize) {
                break;
            }
            List<T> queueItem = queue.removeFirst();
            final int currentTupleSize = queueItem.size();
            for (T item : data.get(currentTupleSize)) {
                List<T> newItem = new ArrayList<T>(queueItem);
                newItem.add(item);
                queue.addLast(newItem);
            }
        }
        return queue;
    }

    // ищет в Map рекурсивно тупик
    public static <K> K findDeadEnd(Map<K, K> map, K end) {
        K next = map.get(end);
        if (next == null)
            return end;
        else
            return findDeadEnd(map, next);
    }

    public static <T> boolean equalArraySets(T[] array1, T[] array2) {
        if (array1.length != array2.length) return false;
        T[] check2 = array2.clone();
        for (T element : array1) {
            boolean found = false;
            for (int i = 0; i < check2.length; i++)
                if (check2[i] != null && BaseUtils.hashEquals(element, check2[i])) {
                    check2[i] = null;
                    found = true;
                    break;
                }
            if (!found) return false;
        }

        return true;
    }


    public static <T> int hashSet(T[] array) {
        int hash = 0;
        for (T element : array)
            hash += element.hashCode();
        return hash;
    }


    public static <T> T nvl(T value1, T value2) {
        return value1 == null ? value2 : value1;
    }

    public static BigDecimal nvl(BigDecimal value1, Double value2) {
        return value1 == null ? BigDecimal.valueOf(value2) : value1;
    }

    public static String evl(String primary, String secondary) {
        return (primary.length() == 0 ? secondary : primary);
    }

    public static String nevl(String primary, String secondary) {
        return primary == null ? secondary : evl(primary, secondary);
    }

    public static boolean hashEquals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1.hashCode() == obj2.hashCode() && obj1.equals(obj2));
    }

    public static <T> boolean contains(T[] array, T element) {
        return contains(array, element, array.length);
    }

    public static <T> boolean contains(T[] array, T element, int num) {
        for (int i = 0; i < num; i++)
            if (array[i].equals(element))
                return true;
        return false;
    }

    public static <T> Set<T> toSet(T... array) {
        return new HashSet<T>(Arrays.asList(array));
    }

    public static <T> T getRandom(List<T> list, Random randomizer) {
        return list.get(randomizer.nextInt(list.size()));
    }

    public static String clause(String clause, String data) {
        return (data.length() == 0 ? "" : " " + clause + " " + data);
    }

    static String clause(String clause, int data) {
        return (data == 0 ? "" : " " + clause + " " + data);
    }

    public static <T, K> OrderedMap<T, K> orderMap(Map<T, K> map, Iterable<T> list) {
        OrderedMap<T, K> result = new OrderedMap<T, K>();
        for (T element : list) {
            K value = map.get(element);
            if (value != null)
                result.put(element, value);
        }
        return result;
    }

    public static <BT, T extends BT> List<T> orderList(Set<T> map, Iterable<BT> list) {
        List<T> result = new ArrayList<T>();
        for (BT element : list)
            if (map.contains(element))
                result.add((T) element);
        return result;
    }

    public static <K, V> OrderedMap<K, V> mergeOrders(OrderedMap<K, V> map1, OrderedMap<K, V> map2) {
        OrderedMap<K, V> result = new OrderedMap<K, V>(map1);
        result.putAll(map2);
        return result;
    }

    public static <V> Map<V, V> mergeMaps(Map<V, V>[] maps) {
        Map<V, V> result = new HashMap<V, V>();
        for (Map<V, V> map : maps)
            result.putAll(map);
        return result;
    }

    public static <T> void replaceListElements(List<T> list, T from, T to) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == from)
                list.set(i, to);
        }
    }

    public static ArrayList<Integer> toListFromArray(int[] ints) {
        ArrayList<Integer> list = new ArrayList();
        for (int i : ints) {
            list.add(i);
        }
        return list;
    }

    public static Object nullZero(String str) {
        return nullBoolean((Integer.parseInt(BaseUtils.nevl(str, "0")) == 1));
    }

    public static Object nullString(String str) {
        if ("".equals(str)) return null;
        else return str;
    }

    public static String nullToString(Object str) {
        if (str == null) return "NULL";
        else return str.toString();
    }

    public static Object nullBoolean(Boolean b) {
        if (b) return true;
        else return null;
    }

    public static Integer nullParseInt(String s) {
        if (s == null) return null;
        else return Integer.parseInt(s);
    }

    public static <K, V> void clearNotKeys(Map<K, V> map, ImSet<? extends K> keep) {
        if (keep.isEmpty())
            map.clear();
        else {
            for (Iterator<K> it = map.keySet().iterator(); it.hasNext(); )
                if (!((ImSet<K>) keep).contains(it.next()))
                    it.remove();
        }
    }

    public static abstract class Group<G, K> {
        public abstract G group(K key);
    }

    public static <G, K> Map<G, Collection<K>> group(Group<G, K> getter, Iterable<K> keys) {
        Map<G, Collection<K>> result = new HashMap<G, Collection<K>>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                Collection<K> groupList = result.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<K>();
                    result.put(group, groupList);
                }
                groupList.add(key);
            }
        }
        return result;
    }

    public static <G, K> Map<G, List<K>> groupList(Group<G, K> getter, List<K> keys) {
        Map<G, List<K>> result = new HashMap<G, List<K>>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                List<K> groupList = result.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<K>();
                    result.put(group, groupList);
                }
                groupList.add(key);
            }
        }
        return result;
    }

    public static <G, K> Map<G, Set<K>> groupSet(Group<G, K> getter, Collection<K> keys) { // assert что keys - set
        Map<G, Set<K>> result = new HashMap<G, Set<K>>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                Set<K> groupSet = result.get(group);
                if (groupSet == null) {
                    groupSet = new HashSet<K>();
                    result.put(group, groupSet);
                }
                groupSet.add(key);
            }
        }
        return result;
    }

    public static <G, K> Map<G, Set<K>> groupSet(final Map<K, G> getter, Set<K> keys) {
        return groupSet(new Group<G, K>() {
            public G group(K key) {
                return getter.get(key);
            }
        }, keys);
    }

    public static <G, K> Map<G, List<K>> groupList(final Map<K, G> getter, List<K> keys) {
        return groupList(new Group<G, K>() {
            public G group(K key) {
                return getter.get(key);
            }
        }, keys);
    }

    public static <G, K> Map<G, Set<K>> groupSet(final Map<K, G> getter) {
        return groupSet(getter, getter.keySet());
    }

    public static <G, K> Map<G, Set<K>> groupSet(final HMap<K, G> getter, Collection<K> keys) {
        return groupSet(new Group<G, K>() {
            public G group(K key) {
                return getter.get(key);
            }
        }, keys);
    }

    public static <G, K> Map<G, List<K>> groupList(final OrderedMap<K, G> getter) {
        return groupList(getter, getter.keyList());
    }

    public static <G, K> SortedMap<G, Set<K>> groupSortedSet(Group<G, K> getter, Collection<K> keys, Comparator<? super G> comparator) { // вообще assert что set
        SortedMap<G, Set<K>> result = new TreeMap<G, Set<K>>(comparator);
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                Set<K> groupSet = result.get(group);
                if (groupSet == null) {
                    groupSet = new HashSet<K>();
                    result.put(group, groupSet);
                }
                groupSet.add(key);
            }
        }
        return result;
    }

    public static <G, K> SortedMap<G, Set<K>> groupSortedSet(final Map<K, G> getter, Comparator<? super G> comparator) {
        return groupSortedSet(new Group<G, K>() {
            public G group(K key) {
                return getter.get(key);
            }
        }, getter.keySet(), comparator);
    }

    public static <G extends GlobalObject, K> SortedMap<G, Set<K>> groupSortedSet(final Map<K, G> getter) {
        return groupSortedSet(getter, GlobalObject.comparator);
    }

    public static <K> Map<K, Integer> multiSet(Collection<K> col) {
        Map<K, Integer> result = new HashMap<K, Integer>();
        for (K element : col) {
            Integer quantity = result.get(element);
            result.put(element, quantity == null ? 1 : quantity + 1);
        }
        return result;
    }

    public static <V> V addValue(Map<V, V> values, V value) {
        V addValue = values.get(value); // смотрим может уже есть
        if (addValue == null) { // если нету, находим рекурсивно первое свободное значение
            addValue = BaseUtils.findDeadEnd(BaseUtils.reverse(values), value);
            values.put(value, addValue);
        }
        return addValue;
    }

    public static <K, V> void putNotNull(K key, Map<K, V> from, Map<K, V> to) {
        V value = from.get(key);
        if (value != null) to.put(key, value);
    }

    public static class Paired<T> {
        public final T[] common;

        private final T[] diff1;
        private final T[] diff2;
        private final boolean invert;

        public T[] getDiff1() {
            return invert ? diff2 : diff1;
        }

        public T[] getDiff2() {
            return invert ? diff1 : diff2;
        }

        public Paired(T[] array1, T[] array2, ArrayInstancer<T> instancer) {
            if (array1.length > array2.length) {
                T[] sw = array2;
                array2 = array1;
                array1 = sw;
                invert = true;
            } else
                invert = false;
            assert array1.length <= array2.length;
            T[] pairedWheres = instancer.newArray(array1.length);
            int pairs = 0;
            T[] thisWheres = instancer.newArray(array1.length);
            int thisnum = 0;
            T[] pairedThatWheres = array2.clone();
            for (T opWhere : array1) {
                boolean paired = false;
                for (int i = 0; i < pairedThatWheres.length; i++)
                    if (pairedThatWheres[i] != null && hashEquals(array2[i], opWhere)) {
                        pairedWheres[pairs++] = opWhere;
                        pairedThatWheres[i] = null;
                        paired = true;
                        break;
                    }
                if (!paired) thisWheres[thisnum++] = opWhere;
            }

            if (pairs == 0) {
                common = instancer.newArray(0);
                diff1 = array1;
                diff2 = array2;
            } else {
                if (pairs == array1.length) {
                    common = array1;
                    diff1 = instancer.newArray(0);
                } else {
                    common = instancer.newArray(pairs);
                    System.arraycopy(pairedWheres, 0, common, 0, pairs);
                    diff1 = instancer.newArray(thisnum);
                    System.arraycopy(thisWheres, 0, diff1, 0, thisnum);
                }

                if (pairs == array2.length)
                    diff2 = diff1;
                else {
                    diff2 = instancer.newArray(array2.length - pairs);
                    int compiledNum = 0;
                    for (T opWhere : pairedThatWheres)
                        if (opWhere != null) diff2[compiledNum++] = opWhere;
                }
            }
        }
    }

    public static <K, V> boolean hashContainsValue(Map<K, V> map, V value) {
        for (Map.Entry<K, V> entry : map.entrySet())
            if (hashEquals(entry.getValue(), value))
                return true;
        return false;
    }

    public static <K> String toString(Collection<K> array, String separator) {
        String result = "";
        for (K element : array)
            result = (result.length() == 0 ? "" : result + separator) + element;
        return result;
    }

    public static <K> String toString(String separator, K... array) {
        String result = "";
        for (K element : array)
            result = (result.length() == 0 ? "" : result + separator) + element;
        return result;
    }

    public static <K, V> boolean containsAll(Map<K, V> map, Map<K, V> contains) {
        for (Map.Entry<K, V> entry : contains.entrySet())
            if (!entry.getValue().equals(map.get(entry.getKey())))
                return false;
        return true;
    }

    public static <K> Map<K, String> mapString(Collection<K> col) {
        Map<K, String> result = new HashMap<K, String>();
        for (K element : col)
            result.put(element, element.toString());
        return result;
    }

    public static Integer[] toObjectArray(int[] a) {
        Integer[] result = new Integer[a.length];
        for (int i = 0; i < a.length; i++)
            result[i] = a[i];
        return result;
    }

    public static Integer[] toOneBasedArray(int[] a) {
        Integer[] result = new Integer[a.length];
        for (int i = 0; i < a.length; i++)
            result[i] = a[i] + 1;
        return result;
    }

    public static Object[] add(Object element, Object[] array1) {
        return add(new Object[]{element}, array1);
    }

    public static Object[] add(Object[] array1, Object element) {
        return add(array1, new Object[]{element});
    }

    public static Object[] add(Object[] array1, Object[] array2) {
        return add(array1, array2, objectInstancer);
    }

    public static Object[] add(List<Object[]> list) {
        int totLength = 0;
        for (Object[] array : list)
            totLength += array.length;
        Object[] result = new Object[totLength];
        int off = 0;
        for (Object[] array : list)
            for (Object object : array)
                result[off++] = object;
        return result;
    }

    public final static ArrayInstancer<Object> objectInstancer = new ArrayInstancer<Object>() {
        public Object[] newArray(int size) {
            return new Object[size];
        }
    };

    public final static ArrayInstancer<String> stringInstancer = new ArrayInstancer<String>() {
        public String[] newArray(int size) {
            return new String[size];
        }
    };

    public static <T> T[] add(T[] array1, T[] array2, ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(array1.length + array2.length);
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static <T> T[] add(List<T> list1, T[] array2, ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(list1.size() + array2.length);
        for (int i = 0; i < list1.size(); i++)
            result[i] = list1.get(i);
        System.arraycopy(array2, 0, result, list1.size(), array2.length);
        return result;
    }

    public static class GenericTypeInstancer<T> implements ArrayInstancer<T> {
        private final Class arrayType;

        public GenericTypeInstancer(Class<T> arrayType) {
            this.arrayType = arrayType;
        }

        public T[] newArray(int size) {
            return (T[]) Array.newInstance(arrayType, size);
        }
    }

    public static <T> T[] addElement(T[] array, T element, Class<T> elementClass) {
        return addElement(array, element, new GenericTypeInstancer<T>(elementClass));
    }

    public static <T> T[] addElement(T element, T[] array, Class<T> elementClass) {
        return addElement(element, array, new GenericTypeInstancer<T>(elementClass));
    }

    public static <T> T[] addElement(T[] array, T element, ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(array.length + 1);

        System.arraycopy(array, 0, result, 0, array.length);
        result[array.length] = element;

        return result;
    }

    public static <T> T[] addElement(T element, T[] array, ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(array.length + 1);

        result[0] = element;
        System.arraycopy(array, 0, result, 1, array.length);

        return result;
    }

    public static int[] addInt(int[] array, int element) {
        int newArr[] = new int[array.length + 1];

        System.arraycopy(array, 0, newArr, 0, array.length);
        newArr[array.length] = element;

        return newArr;
    }

    public static <T> T[] removeElement(T[] array, T element, ArrayInstancer<T> instancer) {
        if (array == null || array.length == 0) {
            return array;
        }

        int ind = -1;
        for (int i = 0; i < array.length; ++i) {
            if (array[i] == element) {
                ind = i;
                break;
            }
        }

        if (ind == -1) {
            return array;
        }

        T[] result = instancer.newArray(array.length - 1);
        System.arraycopy(array, 0, result, 0, ind);
        System.arraycopy(array, ind + 1, result, ind, result.length - ind);

        return result;
    }

    public static <T> T[] genArray(T element, int length, ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(length);
        for (int i = 0; i < length; i++)
            result[i] = element;
        return result;
    }

    public static String[] genArray(String element, int length) {
        return genArray(element, length, stringInstancer);
    }

    public static int[] genArray(int element, int length) {
        int[] ints = new int[length];
        for (int i = 0; i < length; i++)
            ints[i] = element;
        return ints;
    }

    public static boolean isData(Object object) {
        return object instanceof Number || object instanceof String || object instanceof Boolean || object instanceof byte[];
    }

    public static <I, E extends I> List<E> immutableCast(List<I> list) {
        return (List<E>) (List<? extends I>) list;
    }

    public static <K, I, E extends I> Map<K, E> immutableCast(Map<K, I> map) {
        return (Map<K, E>) (Map<K, ? extends I>) map;
    }

    public static <I> I immutableCast(Object object) {
        return (I) object;
    }

    public static <I> I single(Collection<I> col) {
        assert col.size() == 1;
        return col.iterator().next();
    }

    public static <I> I single(Iterable<I> col) {
        Iterator<I> it = col.iterator();
        I result = it.next();
        assert !it.hasNext();
        return result;
    }

    public static <I> I single(I[] array) {
        assert array.length == 1;
        return array[0];
    }

    public static <I> I singleKey(Map<I, ?> map) {
        return BaseUtils.single(map.keySet());
    }

    public static <I> I singleValue(Map<?, I> map) {
        return BaseUtils.single(map.values());
    }

    public static <K, I> Map.Entry<K, I> singleEntry(Map<K, I> map) {
        return BaseUtils.single(map.entrySet());
    }

    public static <K> List<K> reverse(List<K> col) {
        return reverseThis(new ArrayList<K>(col));
    }

    public static <K> List<K> toList(Iterable<K> col) {
        List<K> result = new ArrayList<K>();
        for (K element : col)
            result.add(element);
        return result;
    }

    public static <K> List<K> reverse(Iterable<K> col) {
        return reverse(toList(col));
    }

    public static <K> List<K> reverseThis(List<K> col) {
        Collections.reverse(col);
        return col;
    }

    public static int objectToInt(Integer value) {
        if (value == null)
            return -1;
        else
            return value;
    }

    public static Integer intToObject(int value) {
        if (value == -1)
            return null;
        else
            return value;
    }

    public static String nullTrim(String string) {
        if (string == null)
            return "";
        else
            return string.trim();
    }

    public static String nullEmpty(String string) {
        if (string != null && string.trim().isEmpty())
            return null;
        else
            return string;
    }

    public static String rtrim(String string) {
        int len = string.length();
        while (len > 0 && string.charAt(len - 1) == ' ') len--;
        return string.substring(0, len);
    }

    public static String toCaption(Object name) {
        if (name == null)
            return "";
        else
            return name.toString().trim();
    }

    public static <K, V> Map<K, V> buildMap(Collection<K> col1, Collection<V> col2) {
        assert col1.size() == col2.size();

        Iterator<K> it1 = col1.iterator();
        Iterator<V> it2 = col2.iterator();
        Map<K, V> result = new HashMap<K, V>();
        while (it1.hasNext())
            result.put(it1.next(), it2.next());
        return result;
    }

    public static <K> List<K> toList(K... elements) {
        List<K> list = new ArrayList<K>();
        Collections.addAll(list, elements);
        return list;
    }

    public static <K> List<Boolean> toBooleanList(boolean... elements) {
        List<Boolean> list = new ArrayList<Boolean>();
        for (boolean element : elements)
            list.add(element);
        return list;
    }

    public static <K> List<K> toListNoNull(K... elements) {
        List<K> list = new ArrayList<K>();
        for (K element : elements)
            if (element != null)
                list.add(element);
        return list;
    }

    public static String replicate(char character, int length) {

        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    public static String truncate(String s, int length) {
        return length < s.length() ? s.substring(0, length) : s;
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$" + n + "s", s);
    }

    public static String capitalize(String s) {
        if (s.length() == 0) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static boolean isRedundantString(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static boolean isRedundantString(Object o) {
        return o == null || o.toString().trim().length() == 0;
    }

    public static String spaces(int count) {
        return replicate(' ', count);
    }

    // в отличии от padright дает нуж
    public static String padr(String string, int length) {
        if (length == string.length())
            return string;

        if (length > string.length())
            return string + spaces(length - string.length());

        return string.substring(0, length);
    }

    public static String padl(String string, int length) {
        if (length > string.length())
            return spaces(length - string.length()) + string;
        else
            return string.substring(string.length() - length, string.length());
    }

    public static String padl(String string, int length, char character) {
        if (length > string.length())
            return replicate(character, length - string.length()) + string;
        else
            return string.substring(string.length() - length, string.length());
    }

    public static <K> K last(List<K> list) {
        if (list.size() > 0)
            return list.get(list.size() - 1);
        else
            return null;
    }

    public static <K> int relativePosition(K element, List<K> comparatorList, List<K> insertList) {
        int ins = 0;
        int ind = comparatorList.indexOf(element);

        Iterator<K> icp = insertList.iterator();
        while (icp.hasNext() && comparatorList.indexOf(icp.next()) < ind) {
            ins++;
        }
        return ins;
    }

    public static <T> int[] relativeIndexes(List<T> all, List<T> list) {
        int result[] = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            int index = all.indexOf(list.get(i));
            assert index >= 0;
            result[i] = index;
        }
        return result;
    }

    public static <K> List<K> copyTreeChildren(List children) {
        List<K> result = new ArrayList<K>();
        if (children != null)
            for (Object child : children)
                result.add((K) child);
        return result;
    }

    public static class HashClass<C extends GlobalObject> extends TwinImmutableObject implements GlobalObject {
        private C valueClass;
        private int hash;

        public HashClass(C valueClass, int hash) {
            this.valueClass = valueClass;
            this.hash = hash;
        }

        public HashClass(C valueClass) {
            this(valueClass, 0);
        }

        public boolean calcTwins(TwinImmutableObject o) {
            return hash == ((HashClass) o).hash && valueClass.equals(((HashClass) o).valueClass);
        }

        public int immutableHashCode() {
            return 31 * valueClass.hashCode() + hash;
        }
    }

    // есть в общем то другая схема с генерацией всех перестановок, и поиском минимума (или суммированием)
    public static class HashComponents<K> {
        public final ImMap<K, GlobalObject> map; // или сам класс или HashClass, то есть всегда содержит информацию о классе
        public final int hash;

        public HashComponents(ImMap<K, GlobalObject> map, int hash) {
            this.map = map;
            this.hash = hash;
        }
    }

    public static interface HashInterface<K, C> {

        ImMap<K, C> getParams(); // важно чтобы для C был статичный компаратор

        int hashParams(ImMap<K, ? extends GlobalObject> map);
    }

    // цель минимизировать количество hashParams
    public static <K, C extends GlobalObject> HashComponents<K> getComponents(HashInterface<K, C> hashInterface) {

        final ImMap<K, C> classParams = hashInterface.getParams();
        if (classParams.size() == 0)
            return new HashComponents<K>(MapFact.<K, GlobalObject>EMPTY(), hashInterface.hashParams(MapFact.<K, GlobalObject>EMPTY()));

        MMap<K, GlobalObject> mComponents = MapFact.mMap(classParams, MapFact.<K, GlobalObject>override());

        int resultHash = 0; // как по сути "список" минимальных хэшей
        int compHash = 16769023;

        ImSet<K> freeKeys = null;
        ImOrderMap<C, ImSet<K>> classOrders = classParams.groupValues().sort(BaseUtils.<Comparator<C>>immutableCast(GlobalObject.comparator));
        for (int i = 0, size = classOrders.size(); i < size; i++) {
            freeKeys = classOrders.getValue(i);
            C groupClass = classOrders.getKey(i);

            while (freeKeys.size() > 1) {
                int minHash = Integer.MAX_VALUE;
                MFilterSet<K> mMinKeys = SetFact.mFilter(freeKeys);
                for (K key : freeKeys) {
                    MMap<K, GlobalObject> mMergedComponents = MapFact.mMap(classParams, MapFact.<K, GlobalObject>override()); // замещаем базовые ъэши - новыми
                    mMergedComponents.addAll(mComponents.immutableCopy());
                    mMergedComponents.add(key, new HashClass<C>(groupClass, compHash));

                    int hash = hashInterface.hashParams(mMergedComponents.immutable());
                    if (hash < minHash) { // сбрасываем минимальные ключи
                        mMinKeys = SetFact.mFilter(freeKeys);
                        minHash = hash;
                    }

                    if (hash == minHash) // добавляем в минимальные ключи
                        mMinKeys.keep(key);
                }
                ImSet<K> minKeys = SetFact.imFilter(mMinKeys, freeKeys);

                for (K key : minKeys)
                    mComponents.add(key, new HashClass<C>(groupClass, compHash));

                resultHash = resultHash * 31 + minHash;

                freeKeys = freeKeys.remove(minKeys);

                compHash = MapFact.colHash(compHash * 57793 + 9369319);
            }

            if (freeKeys.size() == 1) // если остался один объект в классе оставляем его с hashCode'ом (для оптимизации)
                mComponents.add(freeKeys.single(), groupClass);
        }
        ImMap<K, GlobalObject> components = mComponents.immutable();

        if (freeKeys.size() == 1) // если остался один объект то финальный хэш не учтен (для оптимизации)
            resultHash = resultHash * 31 + hashInterface.hashParams(components);

        return new HashComponents<K>(components, resultHash);
    }

    public static boolean onlyObjects(Iterable<?> col) {
        for (Object object : col)
            if (!object.getClass().equals(Object.class))
                return false;
        return true;
    }

    public static <T> ImRevMap<T, Object> generateObjects(ImSet<T> col) {
        return col.mapRevValues(new GetValue<Object, T>() {
            public Object getMapValue(T value) {
                return new Object();
            }
        });
    }

    public static void openFile(byte[] data, String extension) throws IOException {
        File file = File.createTempFile("lsf", "." + extension);
        FileOutputStream f = new FileOutputStream(file);
        f.write(data);
        f.close();
        Desktop.getDesktop().open(file);
    }

    public static String firstWord(String string, String separator) {
        int first = string.indexOf(separator);
        if (first >= 0)
            return string.substring(0, first);
        else
            return string;
    }

    public static String encode(int... values) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            for (int i = 0; i < values.length; i++)
                dos.writeInt((values[i] * (27 * (i + 1))) ^ 248979893);
            return Base64.encodeBase64URLSafeString(baos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Integer[] decode(int number, String string) {

        try {
            Integer[] result = new Integer[number];
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(Base64.decodeBase64(string)));
            for (int i = 0; i < number; i++)
                result[i] = (dis.readInt() ^ 248979893) / (27 * (i + 1));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(getString("exceptions.error.decoding.link", string), e);
        }
    }

    public static String[] monthsRussian = new String[]{"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    // приходится складывать в baseUtils, потому что должна быть единая функция и для сервера и для клиента
    // так как отчеты формируются и на сервере
    // используется в *.jrxml
    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(Date date) {
        return formatRussian(date, false, false);
    }

    public static String formatRussian(Date date, TimeZone timeZone) {
        return formatRussian(date, false, false, timeZone);
    }

    public static String formatRussian(Date date, boolean quotes, boolean leadZero) {
        return formatRussian(date, quotes, leadZero, null);
    }

    public static String formatRussian(Date date, boolean quotes, boolean leadZero, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (timeZone != null)
            calendar.setTimeZone(timeZone);
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        if ((leadZero) && (dayOfMonth.length() == 1))
            dayOfMonth = "0" + dayOfMonth;
        if (quotes)
            dayOfMonth = "«" + dayOfMonth + "»";

        return "" + dayOfMonth + " " + monthsRussian[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR);
    }

    public static String[] monthsEnglish = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    public static int getNumberOfMonthEnglish(String month) {
        for (int i = 0; i < monthsEnglish.length; i++)
            if (month.equals(monthsEnglish[i]))
                return i + 1;
        return 1;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatEnglish(Date date) {

        // todo : сделать форматирование по timeZone сервера

        return formatEnglish(date, false, false);
    }

    public static String formatEnglish(Date date, boolean quotes, boolean leadZero) {
        return formatEnglish(date, quotes, leadZero, null);
    }

    public static String formatEnglish(Date date, boolean quotes, boolean leadZero, TimeZone timeZone) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (timeZone != null)
            calendar.setTimeZone(timeZone);
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        if ((leadZero) && (dayOfMonth.length() == 1))
            dayOfMonth = "0" + dayOfMonth;
        if (quotes)
            dayOfMonth = "“" + dayOfMonth + "”";

        return "" + monthsEnglish[calendar.get(Calendar.MONTH)] + " " + dayOfMonth + ", " + calendar.get(Calendar.YEAR);

    }

    public static String justInitials(String fullName, boolean lastNameFirst, boolean revert) {
        String[] names = fullName.split(" ");
        String initials = "", lastName = "";
        if (lastNameFirst) {
            lastName = names[0];
            for (int i = 1; i < names.length; i++)
                if (!names[i].isEmpty()) {
                    if (!initials.isEmpty())
                        initials += ' ';
                    initials += names[i].charAt(0) + ".";
                }
            if (revert)
                return initials + ' ' + lastName;
            else
                return lastName + ' ' + initials;
        } else {
            for (int i = 0; i < names.length - 1; i++)
                if (!names[i].isEmpty()) {
                    if (!initials.isEmpty())
                        initials += " ";
                    initials += names[i].charAt(0) + ".";
                }
            if (names.length > 0)
                lastName = names[names.length - 1];
            if (revert)
                return lastName + ' ' + initials;
            else
                return initials + ' ' + lastName;
        }
    }

    public static String[] feminineNumbers = new String[]{"ноль", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять", "десять"};

    public static String intToFeminine(int number) {
        if ((number >= 0) && (number <= 10))
            return feminineNumbers[number];
        else return String.valueOf(number);
    }

    public static Date getFirstDateInMonth(int year, int month) {
        return new GregorianCalendar(year, month - 1, 1, 0, 0, 0).getTime();
    }

    public static Date getLastDateInMonth(int year, int month) {
        Calendar calendar = new GregorianCalendar(year, month - 1, 1, 0, 0, 0);
        calendar.roll(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf(".");
        String extension = (index == -1) ? "" : name.substring(index + 1);
        return extension;
    }

    public static byte[] filesToBytes(boolean multiple, boolean storeName, boolean custom, File... files) throws IOException {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteOutStream);

        byte result[] = null;
        try {
            if (multiple)
                outStream.writeInt(files.length);
            for (File file : files) {
                if (storeName) {
                    outStream.writeInt(file.getName().length());
                    outStream.writeBytes(file.getName());
                }
                byte fileBytes[] = IOUtils.getFileBytes(file);
                byte ext[] = new byte[0];
                //int length = fileBytes.length;

                if (custom) {
                    ext = getFileExtension(file).getBytes();
                }
                byte[] union = mergeFileAndExtension(fileBytes, ext);

                if (multiple)
                    outStream.writeInt(union.length);
                outStream.write(union);

                if (!multiple) // just in case
                    break;
            }

            result = byteOutStream.toByteArray();
            outStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static byte[] mergeFileAndExtension(byte[] file, byte[] ext) {
        byte[] extBytes = new byte[0];
        if (ext.length != 0) {
            extBytes = new byte[ext.length + 1];
            extBytes[0] = (byte) ext.length;
            System.arraycopy(ext, 0, extBytes, 1, ext.length);
        }
        byte[] result = new byte[extBytes.length + file.length];
        System.arraycopy(extBytes, 0, result, 0, extBytes.length);
        System.arraycopy(file, 0, result, extBytes.length, file.length);
        return result;
    }

    public static String getExtension(byte[] array) {
        byte ext[] = new byte[array[0]];
        System.arraycopy(array, 1, ext, 0, ext.length);
        return new String(ext);
    }

    public static byte[] getFile(byte[] array) {
        byte file[] = new byte[array.length - array[0] - 1];
        System.arraycopy(array, 1 + array[0], file, 0, file.length);
        return file;
    }

    public static byte[] bytesToBytes(byte[]... files) {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(byteOutStream);

        byte result[] = null;
        try {
            outStream.writeInt(files.length);
            for (byte[] file : files) {


                outStream.writeInt(file.length);
                outStream.write(file);
            }

            result = byteOutStream.toByteArray();
            outStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }


    public static int[] consecutiveInts(int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            result[i] = i;
        }
        return result;
    }

    public static int[] toPrimitive(List<Integer> array) {
        if (array == null) {
            return null;
        }
        final int[] result = new int[array.size()];
        int i = 0;
        for (int a : array) {
            result[i++] = a;
        }
        return result;
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static int cmp(int a, int b, boolean max) {
        return max ? max(a, b) : min(a, b);
    }

    public static boolean cmp(boolean a, boolean b, boolean max) {
        return max ? a || b : a && b;
    }

    private static SimpleAddValue<Object, Integer> addMinInt = new SymmAddValue<Object, Integer>() {
        public Integer addValue(Object key, Integer prevValue, Integer newValue) {
            return BaseUtils.min(prevValue, newValue);
        }
    };

    public static <K> SimpleAddValue<K, Integer> addMinInt() {
        return (SimpleAddValue<K, Integer>) addMinInt;
    }

    public static int min(int a, int b) {
        return a > b ? b : a;
    }

    public static long max(long a, long b) {
        return a > b ? a : b;
    }

    public static double min(double a, double b) {
        return a > b ? b : a;
    }

    public static List<Integer> consecutiveList(int i, int is) {
        List<Integer> result = new ArrayList<Integer>();
        for (int j = 0; j < i; j++)
            result.add(j + is);
        return result;
    }

    public static List<Integer> consecutiveList(int i) {
        return consecutiveList(i, 1);
    }

    public static <K> List<K> sort(Collection<K> col, Comparator<K> comparator) {
        List<K> list = new ArrayList<K>(col);
        Collections.sort(list, comparator);
        return list;
    }

    public static <K> FunctionSet<K> merge(FunctionSet<K>... sets) {
        FunctionSet<K> result = sets[0];
        for (int i = 1; i < sets.length; i++)
            result = merge(result, sets[i]);
        return result;
    }

    public static <K> FunctionSet<K> merge(FunctionSet<K> set1, FunctionSet<K> set2) {
        if (set1.isEmpty() || set2.isFull())
            return set2;
        if (set2.isEmpty() || set1.isFull())
            return set1;
//        if(set1 instanceof ImSet && set2 instanceof ImSet)
//            return ((ImSet<K>)set1).merge((ImSet<K>)set2);
        return new MergeFunctionSet<K>(set1, set2);
    }

    public static <T> FunctionSet<T> universal(boolean empty) {
        if (empty)
            return SetFact.EMPTY();
        else
            return FullFunctionSet.instance();
    }

    public static <MK, K, V> void putUpdate(Map<MK, Map<K, V>> keyValues, MK key, Map<K, V> values, boolean update) {
        if (update)
            keyValues.put(key, BaseUtils.<K, K, K, V>override(keyValues.get(key), values));
        else
            keyValues.put(key, values);
    }

    public static <K, V, M extends Map<K, V>> M getNearObject(V findValue, List<M> keys) {
        if (keys.size() <= 1)
            return null;

        M nearObject = null;
        for (M key : keys) {
            if (key.values().contains(findValue) && nearObject == null) {
                int index = keys.indexOf(key);
                index = index == keys.size() - 1 ? index - 1 : index + 1;
                nearObject = keys.get(index);
            }
        }
        return nearObject;
    }

    public static <K, V, M extends Map<K, V>> V getNearValue(K findKey, V findValue, List<M> keys) {
        M nearObject = getNearObject(findValue, keys);
        if (nearObject != null)
            return nearObject.get(findKey);
        return null;
    }

    private static final char[] randomsymbols = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private final static SecureRandom random = new SecureRandom();

    public static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(randomsymbols[random.nextInt(randomsymbols.length)]);
        }
        return sb.toString();
    }

    public static String calculateBase64Hash(String algorithm, String input, Object salt) throws RuntimeException {
        try {
            return new String(Base64.encodeBase64(calculateHash(algorithm, input, salt).getBytes("UTF-8")), Charset.forName("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String calculateHash(String algorithm, String input, Object salt) throws RuntimeException {
        try {
            return new String(MessageDigest.getInstance(algorithm).digest(mergePasswordAndSalt(input, salt).getBytes("UTF-8")), Charset.forName("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String mergePasswordAndSalt(String password, Object salt) {
        if (password == null) {
            password = "";
        }
        if ((salt == null) || "".equals(salt)) {
            return password;
        } else {
            return password + "{" + salt.toString() + "}";
        }
    }

    public static int compareInts(int a, int b) {
        return a < b
                ? -1
                : a > b
                ? 1 : 0;
    }

    public static void runLater(final int delay, final Runnable runnable) {
        Thread thread = new Thread("runLater-thread") {
            @Override
            public void run() {
                SystemUtils.sleep(delay);
                runnable.run();
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    public static <T> void addToOrderedList(List<T> orderedList, T element, int after, Comparator<T> compare) {
        assert after <= orderedList.size();
        while (true) {
            if (after >= orderedList.size() || compare.compare(orderedList.get(after), element) > 0) {
                orderedList.add(after, element);
                break;
            }
            after++;
        }
    }

    public static String bigDecimalToString(BigDecimal bd) {
        return bigDecimalToString("#,##0.###", bd);
    }

    public static String bigDecimalToString(String format, BigDecimal bd) {
        return new DecimalFormat(format).format(bd);
    }

    public static String dateToString(Date d) {
        return dateToString("dd/MM/yyyy", d);
    }

    public static String dateToString(String format, Date d) {
        return new SimpleDateFormat(format).format(d);
    }
}

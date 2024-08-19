package lsfusion.base;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.base.comb.map.GlobalObject;
import lsfusion.base.file.*;
import lsfusion.base.lambda.ArrayInstancer;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.MergeFunctionSet;
import lsfusion.base.lambda.set.RemoveFunctionSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.session.ExternalUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.spi.DateFormatProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static lsfusion.base.ApiResourceBundle.getString;

public class BaseUtils {
    public static final Logger systemLogger = Logger.getLogger("SystemLogger");
    public static final Logger serviceLogger = Logger.getLogger("ServiceLogger");

    //Длина строки может быть маскимум 65535, каждый символ может занимать от 1 до 3х байт
    //используем пессимистичный вариант, чтобы не заниматься реальным рассчётом длины, т.к. это долго 
    private static final int STRING_SERIALIZATION_CHUNK_SIZE = 65535/3;

    public static Integer getApiVersion() {
        return 300;
    }

    public static String getPlatformVersion() {
        try {
            return org.apache.commons.io.IOUtils.toString(BaseUtils.class.getResourceAsStream("/lsfusion.version"));
        } catch (IOException e) {
            systemLogger.error("Error reading platform version", e);
            return null;
        }
    }

    public static String checkClientVersion(String serverPlatformVersion, Integer serverApiVersion, String clientPlatformVersion, Integer clientApiVersion) {
        if (!clientApiVersion.equals(serverApiVersion)) {
            String serverVersion = serverPlatformVersion + " [" + serverApiVersion + "]";
            String clientVersion = clientPlatformVersion + " [" + clientApiVersion + "]";
            String needUpdate = clientApiVersion < serverApiVersion ? getString("check.client.version.client") : getString("check.client.version.server");
            return getString("check.client.version", serverVersion, clientVersion) + " " + needUpdate;
        } else return null;
    }

    public static boolean nullEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        return obj1.equals(obj2);
    }

    public static int nullCompareTo(Comparable obj1, Comparable obj2) {
        return obj1 == null ? (obj2 == null ? 0 : -1) : (obj2 == null ? 1 : obj1.compareTo(obj2));
    }

    public static boolean nullHashEquals(Object obj1, Object obj2) {
        if (obj1 == null)
            return obj2 == null;
        return obj2 != null && hashEquals(obj1, obj2);
    }

    public static int nullHash(Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    public static <BK, K extends BK, V> Map<K, V> filterInclKeys(Map<BK, V> map, Set<? extends K> keys) {
        if(keys.size() == map.size()) { // optimization
            assert keys.equals(map.keySet());
            return (Map<K, V>) map;
        }

        Map<K, V> result = new HashMap<>();
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    public static <BK, K extends BK, V> Map<K, V> filterKeys(Map<BK, V> map, Iterable<? extends K> keys) {
        Map<K, V> result = new HashMap<>();
        for (K key : keys) {
            V value = map.get(key);
            if (value != null) result.put(key, value);
        }
        return result;
    }

    public static <BK, K extends BK, V> Map<K, V> filterKeys(Map<BK, V> map, FunctionSet<K> filter, Class<K> aClass) {
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<BK, V> entry : map.entrySet()) {
            if (aClass.isInstance(entry.getKey())) {
                K key = (K) entry.getKey();
                if (filter.contains(key)) {
                    result.put(key, entry.getValue());
                }
            }
        }
        return result;
    }

    public static <E> Iterable<E> filterIterable(final Iterable<E> iterable, final FunctionSet<E> filter) {
        return new Iterable<E>() {
            @Override
            public Iterator<E> iterator() {
                final Iterator<E> iterator = iterable.iterator();
                return new Iterator<E>() {
                    E next;
                    private void checkNext() {
                        if(next == null) {
                            while (iterator.hasNext()) {
                                next = iterator.next();
                                if (filter.contains(next))
                                    return;
                                else
                                    next = null;
                            }
                        }
                    }
                    @Override
                    public boolean hasNext() {
                        checkNext();
                        
                        return next != null;
                    }

                    @Override
                    public E next() {
                        checkNext();
                        
                        E result = next;
                        next = null;
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public static <K, T extends K, V> Map<K, V> filterNotKeys(Map<K, V> map, FunctionSet<T> keys, Class<T> aClass) {
        Map<K, V> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (!aClass.isInstance(entry.getKey()) || !((FunctionSet<K>) keys).contains(entry.getKey()))
                result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V> Map<V, K> reverse(Map<K, V> map) {
        return reverse(map, false);
    }

    public static <K, V> Map<V, K> reverse(Map<K, V> map, boolean ignoreUnique) {
        Map<V, K> result = new HashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            assert ignoreUnique || !result.containsKey(entry.getValue());
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    public static <K, V> Map<K, V> toMap(Collection<K> collection, V value) {
        Map<K, V> result = new HashMap<>();
        for (K object : collection)
            result.put(object, value);
        return result;
    }

    public static Map<String, String> toStringMap(String[] keys, String[] values) {
        Map<String, String> result = new HashMap<>();
        if (keys != null && values != null) {
            for (int i = 0; i < keys.length; i++) {
                result.put(keys[i], values[i]);
            }
        }
        return result;
    }

    public static Object deserializeObject(byte[] state) throws IOException {
        return deserializeObject(new DataInputStream(new ByteArrayInputStream(state)));
    }

    public static Object deserializeObject(DataInputStream inStream) throws IOException {
        return deserializeObject(inStream, inStream.readByte());
    }

    public static Object deserializeObject(DataInputStream inStream, int objectType) throws IOException {

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
            int len = inStream.readInt();
            return new RawFileData(inStream, len);
        }

        if (objectType == 7) {
            int len = inStream.readInt();
            return new FileData(IOUtils.readBytesFromStream(inStream, len));
        }

        if (objectType == 8) {
            return new Color(inStream.readInt());
        }

        if (objectType == 9) {
            return deserializeBigDecimal(inStream);
        }

        if (objectType == 10) {
            return LocalDate.of(inStream.readInt(), inStream.readInt(), inStream.readInt());
        }

        if (objectType == 11) {
            return LocalTime.of(inStream.readInt(), inStream.readInt(), inStream.readInt(), inStream.readInt());
        }

        if (objectType == 12) {
            return LocalDateTime.of(inStream.readInt(), inStream.readInt(), inStream.readInt(),
                    inStream.readInt(), inStream.readInt(),inStream.readInt(), inStream.readInt());
        }

        if(objectType == 13) {
            return Instant.ofEpochMilli(inStream.readLong());
        }

        if (objectType == 14) {
            int len = inStream.readInt();
            return new NamedFileData(IOUtils.readBytesFromStream(inStream, len));
        }

        if (objectType == 15) {
            int size = inStream.readInt();
            String[] prefixes = new String[size + 1];
            for(int i = 0; i < size + 1; i++) {
                prefixes[i] = inStream.readUTF();
            }
            Serializable[] files = new Serializable[size];
            for(int i = 0; i < size; i++) {
                if(inStream.readBoolean()) {
                    String name = inStream.readUTF();
                    int fileLength = inStream.readInt();
                    files[i] = new StringWithFiles.File(new RawFileData(inStream, fileLength), name);
                } else
                    files[i] = IOUtils.readAppImage(inStream);
            }
            String rawString = inStream.readUTF();

            return new StringWithFiles(prefixes, files, rawString);
        }

        if (objectType == 16) {
            return IOUtils.readAppImage(inStream);
        }

        if (objectType == 17) {
            return deserializeString(inStream);
        }

        if (objectType == 18) {
            return IOUtils.readAppFileDataImage(inStream);
        }

        throw new IOException();
    }

    public static String deserializeString(DataInputStream inStream) throws IOException {
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

    public static byte[] serializeObject(Object value) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serializeObject(new DataOutputStream(outStream), value);
        return outStream.toByteArray();
    }

    public static boolean findInCamelCase(String string, Predicate<String> test) {
        StringBuilder word = new StringBuilder();
        for(int i = 0, length = string.length(); i < length; i++) {
            char ch = string.charAt(i);
            if(Character.isUpperCase(ch)) {
                if (test.test(word.toString()))
                    return true;
                word = new StringBuilder();
                word.append(Character.toLowerCase(ch));
            } else
                word.append(ch);
        }
        return test.test(word.toString());
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

        if (object instanceof RawFileData) {
            outStream.writeByte(6);
            byte[] obj = ((RawFileData) object).getBytes();
            outStream.writeInt(obj.length);
            outStream.write(obj);
            return;
        }

        if (object instanceof FileData) {
            outStream.writeByte(7);
            byte[] obj = ((FileData) object).getBytes();
            outStream.writeInt(obj.length);
            outStream.write(obj);
            return;
        }

        if (object instanceof Color) {
            outStream.writeByte(8);
            outStream.writeInt(((Color) object).getRGB());
            return;
        }

        if (object instanceof BigDecimal) {
            outStream.writeByte(9);
            serializeBigDecimal(outStream, (BigDecimal) object);
            return;
        }

        if (object instanceof LocalDate) {
            outStream.writeByte(10);
            outStream.writeInt(((LocalDate) object).getYear());
            outStream.writeInt(((LocalDate) object).getMonthValue());
            outStream.writeInt(((LocalDate) object).getDayOfMonth());
            return;
        }

        if (object instanceof LocalTime) {
            outStream.writeByte(11);
            outStream.writeInt(((LocalTime) object).getHour());
            outStream.writeInt(((LocalTime) object).getMinute());
            outStream.writeInt(((LocalTime) object).getSecond());
            outStream.writeInt(((LocalTime) object).getNano());
            return;
        }

        if (object instanceof LocalDateTime) {
            outStream.writeByte(12);
            outStream.writeInt(((LocalDateTime) object).getYear());
            outStream.writeInt(((LocalDateTime) object).getMonthValue());
            outStream.writeInt(((LocalDateTime) object).getDayOfMonth());
            outStream.writeInt(((LocalDateTime) object).getHour());
            outStream.writeInt(((LocalDateTime) object).getMinute());
            outStream.writeInt(((LocalDateTime) object).getSecond());
            outStream.writeInt(((LocalDateTime) object).getNano());
            return;
        }

        if(object instanceof Instant) {
            outStream.writeByte(13);
            outStream.writeLong(((Instant) object).toEpochMilli());
            return;
        }

        if (object instanceof NamedFileData) {
            outStream.writeByte(14);
            byte[] obj = ((NamedFileData) object).getBytes();
            outStream.writeInt(obj.length);
            outStream.write(obj);
            return;
        }

        if (object instanceof StringWithFiles) {
            outStream.writeByte(15);

            StringWithFiles stringWithFiles = (StringWithFiles) object;

            outStream.writeInt(stringWithFiles.files.length);
            for(String prefix : stringWithFiles.prefixes) {
                outStream.writeUTF(prefix);
            }
            for(Serializable data : stringWithFiles.files) {
                if (data instanceof StringWithFiles.File) {
                    outStream.writeBoolean(true);

                    StringWithFiles.File file = (StringWithFiles.File) data;
                    outStream.writeUTF(file.name);
                    byte[] obj = file.raw.getBytes();
                    outStream.writeInt(obj.length);
                    outStream.write(obj);
                } else { // it's an image
                    outStream.writeBoolean(false);

                    IOUtils.writeAppImage(outStream, (AppImage) data);
                }
            }
            outStream.writeUTF(stringWithFiles.rawString);
            return;
        }

        if (object instanceof AppImage) {
            outStream.writeByte(16);
            IOUtils.writeAppImage(outStream, (AppImage) object);
            return;
        }

        if (object instanceof java.sql.Array) {
            outStream.writeByte(17);
            serializeString(outStream, object.toString());
            return;
        }

        if (object instanceof AppFileDataImage) {
            outStream.writeByte(18);
            IOUtils.writeAppFileDataImage(outStream, (AppFileDataImage) object);
            return;
        }

        throw new IOException();
    }// -------------------------------------- Сериализация классов -------------------------------------------- //

    public static byte[] serializeCustomObject(Object object) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(b);
        o.writeObject(object);
        return b.toByteArray();
    }

    public static Object deserializeCustomObject(byte[] bytes) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream o = new ObjectInputStream(b);
        try {
            return o.readObject();
        } catch (ClassNotFoundException e) {
            throw Throwables.propagate(e);
        }
    }
    
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

    public static void serializeBigDecimal(DataOutputStream outStream, BigDecimal number) throws IOException {
        if (number == null) {
            outStream.writeInt(-1);
        } else {
            byte[] numberArray = number.unscaledValue().toByteArray();
            outStream.writeInt(numberArray.length);
            outStream.write(numberArray);
            outStream.writeInt(number.scale());
        }
    }

    public static BigDecimal deserializeBigDecimal(DataInputStream inStream) throws IOException {
        int arrayLen = inStream.readInt();
        if (arrayLen < 0) {
            return null;
        }

        byte[] numberArray = new byte[arrayLen];
        inStream.read(numberArray);
        BigInteger intNumber = new BigInteger(numberArray);
        int scale = inStream.readInt();
        return new BigDecimal(intNumber, scale);
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
        Map<K, V> removeMap = new HashMap<>();
        for (Map.Entry<K, V> property : map.entrySet())
            if (!remove.contains(property.getKey()))
                removeMap.put(property.getKey(), property.getValue());
        return removeMap;
    }

    public static <K> Collection<K> add(Collection<? extends K> col, K add) {
        Collection<K> result = new ArrayList<>(col);
        result.add(add);
        return result;
    }

    public static <K> Set<K> addSet(Set<? extends K> col, K add) {
        Set<K> result = new HashSet<>(col);
        result.add(add);
        return result;
    }

    public static <K> List<K> add(List<K> col, K add) {
        ArrayList<K> result = new ArrayList<>(col);
        result.add(add);
        return result;
    }

    public static <K, V> Map<K, V> add(Map<? extends K, ? extends V> map, K add, V addValue) {
        Map<K, V> result = new HashMap<>(map);
        result.put(add, addValue);
        return result;
    }

    public static <K> List<K> add(K add, List<? extends K> col) {
        ArrayList<K> result = new ArrayList<>();
        result.add(add);
        result.addAll(col);
        return result;
    }

    public static <K> List<K> remove(List<? extends K> set, int remove) {
        List<K> result = new ArrayList<>(set);
        result.remove(remove);
        return result;
    }

    public static <K> List<K> removeList(List<K> list, Collection<K> remove) {
        List<K> removeList = new ArrayList<>();
        for (K property : list)
            if (!remove.contains(property))
                removeList.add(property);
        return removeList;
    }

    public static <K> List<K> removeList(List<K> list, K remove) {
        return removeList(list, Collections.singleton(remove));
    }

    public static <K> K lastSetElement(Set<K> set) {
        K key = null;
        for (K k : set) {
            key = k;
        }
        return key;
    }

    public static <B, K1 extends B, K2 extends B, V> LinkedHashMap<B, V> mergeLinked(LinkedHashMap<K1, ? extends V> map1, LinkedHashMap<K2, ? extends V> map2) {
        LinkedHashMap<B, V> result = new LinkedHashMap<>(map1);
        for (Map.Entry<K2, ? extends V> entry2 : map2.entrySet()) {
            V prevValue = result.put(entry2.getKey(), entry2.getValue());
            assert prevValue == null || prevValue.equals(entry2.getValue());
        }
        return result;
    }

    public static <B, K1 extends B, K2 extends B, V> Map<B, V> merge(Map<K1, ? extends V> map1, Map<K2, ? extends V> map2) {
        if(map2.isEmpty())
            return (Map<B, V>) map1;
        if(map1.isEmpty())
            return (Map<B, V>) map2;

        Map<B, V> result = new HashMap<>(map1);
        for (Map.Entry<K2, ? extends V> entry2 : map2.entrySet()) {
            V prevValue = result.put(entry2.getKey(), entry2.getValue());
            assert prevValue == null || prevValue.equals(entry2.getValue());
        }
        return result;
    }

    public static <B, K1 extends B, K2 extends B, V> Map<B, V> override(Map<K1, ? extends V> map1, Map<K2, ? extends V> map2) {
        Map<B, V> result = new HashMap<>(map1);
        result.putAll(map2);
        return result;
    }

    public static <B, K1 extends B, K2 extends B> Collection<B> merge(Collection<K1> col1, Collection<K2> col2) {
        Collection<B> result = new ArrayList<>(col1);
        result.addAll(col2);
        return result;
    }

    public static <B> List<B> addList(B item, List<? extends B> list) {
        List<B> result = new ArrayList<>();
        result.add(item);
        result.addAll(list);
        return result;
    }

    public static <B> List<B> addList(B item1, B item2, List<? extends B> list) {
        List<B> result = new ArrayList<>();
        result.add(item1);
        result.add(item2);
        result.addAll(list);
        return result;
    }

    public static <B> List<B> mergeList(List<? extends B> list1, List<? extends B> list2) {
        List<B> result = new ArrayList<>(list1);
        result.addAll(list2);
        return result;
    }

    @SafeVarargs
    public static <B> List<B> mergeLists(List<B>... lists) {
        List<B> result = new ArrayList<>();
        for (List<B> list : lists) {
            result.addAll(list);
        }
        return result;
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

    public static boolean hashEquals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1.hashCode() == obj2.hashCode() && obj1.equals(obj2));
    }

    public static String clause(String clause, String data) {
        return (data.length() == 0 ? "" : " " + clause + " " + data);
    }

    public static <T> boolean replaceListElements(List<T> list, ImMap<T, T> to) {
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            T toElement = to.get(list.get(i));
            if (toElement != null) {
                list.set(i, toElement);
                replaced = true;
            }
        }
        return replaced;
    }

    public static long nullToZero(Long value) {
        return value == null ? 0 : value;
    }

    public static String nullToString(Object str) {
        if (str == null) return "NULL";
        else return str.toString();
    }

    public static Object nullBoolean(Boolean b) {
        if (b) return true;
        else return null;
    }

    public static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static String replaceSeparators(String value, char separator, char groupingSeparator) {
        if (value.contains(",") && groupingSeparator != ',' && separator == '.')
            value = replaceCommaSeparator(value);
        else if (value.contains(".") && groupingSeparator != '.' && separator == ',')
            value = replaceDotSeparator(value);
        return value;
    }

    public static String replaceCommaSeparator(String value) {
        return value.replace(',', '.');
    }

    public static String replaceDotSeparator(String value) {
        return value.replace('.', ',');
    }

    public static <K, T extends K, V> void clearNotKeys(Map<K, V> map, FunctionSet<T> keep, Class<T> aClass) {
        if (keep.isEmpty())
            map.clear();
        else {
            map.keySet().removeIf(element -> !aClass.isInstance(element) || !((FunctionSet<K>) keep).contains(element));
        }
    }

    public static double nullAdd(Double p1, Double p2) {
        double result = 0.0;
        if(p1 != null)
            result += p1;
        if(p2 != null)
            result += p2;
        return result;
    }

    @FunctionalInterface
    public interface Group<G, K> {
        G group(K key);
    }

    public static <G, K> Map<G, Collection<K>> group(Group<G, K> getter, Iterable<K> keys) {
        Map<G, Collection<K>> result = new HashMap<>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                Collection<K> groupList = result.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    result.put(group, groupList);
                }
                groupList.add(key);
            }
        }
        return result;
    }

    public static <G, K> Map<G, List<K>> groupList(Group<G, K> getter, List<K> keys) {
        Map<G, List<K>> result = new HashMap<>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null) {
                List<K> groupList = result.get(group);
                if (groupList == null) {
                    groupList = new ArrayList<>();
                    result.put(group, groupList);
                }
                groupList.add(key);
            }
        }
        return result;
    }

    public static <G, K> Map<G, K> groupListFirst(Group<G, K> getter, List<K> keys) {
        Map<G, K> result = new HashMap<>();
        for (K key : keys) {
            G group = getter.group(key);
            if (group != null && !result.containsKey(group))
                result.put(group, key);
        }
        return result;
    }

    public static <G, K> Map<G, Long> groupSum(Group<G, K> getter, Map<K, Long> map) { // assert что keys - set
        Map<G, Long> result = new HashMap<>();
        for (Map.Entry<K, Long> entry : map.entrySet()) {
            K key = entry.getKey();
            G group = getter.group(key);
            result.put(group, nvl(result.get(group),0L) + entry.getValue());
        }
        return result;
    }

    public static <G, K> Map<G, List<K>> groupList(final Map<K, G> getter, List<K> keys) {
        return groupList(getter::get, keys);
    }

    public static <G, K> Map<G, List<K>> groupList(final OrderedMap<K, G> getter) {
        return groupList(getter, getter.keyList());
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

    public final static ArrayInstancer<Object> objectInstancer = Object[]::new;

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

    public static <T> T[] addElement(T element, T[] array, Class<T> elementClass) {
        return addElement(element, array, new GenericTypeInstancer<>(elementClass));
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

    public static <T> T[] genArray(T element, int length, ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(length);
        for (int i = 0; i < length; i++)
            result[i] = element;
        return result;
    }

    public static <I, E extends I> List<E> immutableCast(List<I> list) {
        return (List<E>) list;
    }

    public static <K, I, E extends I> Map<K, E> immutableCast(Map<K, I> map) {
        return (Map<K, E>) map;
    }

    public static <I> I mutableCast(Object object) {
        return (I) object;
    }
    public static <I> I immutableCast(Object object) {
        return (I) object;
    }
    
    public static String tab(String tab) {
        return tabPrefix(tab, "\t");
    }
    public static String tabPrefix(String tab, String prefix) {
        return tab.replace("\n", "\n" + prefix);
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
        return reverseThis(new ArrayList<>(col));
    }

    public static <K> List<K> toList(Iterable<K> col) {
        List<K> result = new ArrayList<>();
        for (K element : col)
            result.add(element);
        return result;
    }

    public static <K> List<K> reverseThis(List<K> col) {
        Collections.reverse(col);
        return col;
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

    @SafeVarargs
    public static <K> List<K> toList(K... elements) {
        List<K> list = new ArrayList<>();
        Collections.addAll(list, elements);
        return list;
    }

    public static String replicate(char character, int length) {

        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    public static byte[] getSafeBytes(String string, String charset) {
        try {
            return string.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }
    public static String toSafeString(byte[] array, String charset) {
        try {
            return new String(array, charset);
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    // base64 encodings
    public static byte[] getHashBytes(String string) {
        return string.getBytes(ExternalUtils.hashCharset);
    }
    public static String toHashString(byte[] array) {
        return new String(array, ExternalUtils.hashCharset);
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
    
    public static int countRepeatingChars(String string, char character, int index) {
        int count = 0;
        for (int i = index; i < string.length(); i++) {
            if (string.charAt(i) != character) {
                break;
            }
            count++;
        }
        return count;
    }

    public static int countOccurrences(String string, char character) {
        int count = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == character)
                count++;
        }
        return count;
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

    public static <K> List<K> copyTreeChildren(List children) {
        List<K> result = new ArrayList<>();
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

    public interface HashInterface<K, C> {

        ImMap<K, C> getParams(); // важно чтобы для C был статичный компаратор

        int hashParams(ImMap<K, ? extends GlobalObject> map);
    }

    // цель минимизировать количество hashParams
    public static <K, C extends GlobalObject> HashComponents<K> getComponents(HashInterface<K, C> hashInterface) {

        final ImMap<K, C> classParams = hashInterface.getParams();
        if (classParams.size() == 0)
            return new HashComponents<>(MapFact.EMPTY(), hashInterface.hashParams(MapFact.EMPTY()));

        MMap<K, GlobalObject> mComponents = MapFact.mMap(classParams, MapFact.override());

        int resultHash = 0; // как по сути "список" минимальных хэшей
        int compHash = 16769023;

        ImSet<K> freeKeys = null;
        ImOrderMap<C, ImSet<K>> classOrders = classParams.groupValues().sort(BaseUtils.immutableCast(GlobalObject.comparator));
        for (int i = 0, size = classOrders.size(); i < size; i++) {
            freeKeys = classOrders.getValue(i);
            C groupClass = classOrders.getKey(i);

            while (freeKeys.size() > 1) {
                int minHash = Integer.MAX_VALUE;
                MFilterSet<K> mMinKeys = SetFact.mFilter(freeKeys);
                for (K key : freeKeys) {
                    MMap<K, GlobalObject> mMergedComponents = MapFact.mMap(classParams, MapFact.override()); // замещаем базовые ъэши - новыми
                    mMergedComponents.addAll(mComponents.immutableCopy());
                    mMergedComponents.add(key, new HashClass<>(groupClass, compHash));

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
                    mComponents.add(key, new HashClass<>(groupClass, compHash));

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

        return new HashComponents<>(components, resultHash);
    }

    public static boolean onlyObjects(Iterable<?> col) {
        for (Object object : col)
            if (!object.getClass().equals(Object.class))
                return false;
        return true;
    }

    public static <T> ImRevMap<T, Object> generateObjects(ImSet<T> col) {
        return col.mapRevValues((T value) -> new Object());
    }

    public static void openFile(RawFileData data, String name, String extension) throws IOException {
        File file = name != null ? new File(System.getProperty("java.io.tmpdir") + "/" + getFileName(name, extension)) : File.createTempFile("lsf", "." + extension);
        try (FileOutputStream f = new FileOutputStream(file)) {
            data.write(f);
        }

            ///Можно ждать, пока пользователь закроет файл

            //https://mvnrepository.com/artifact/org.apache.commons/commons-exec
            //https://stackoverflow.com/questions/847838/launch-file-from-java
            //https://stackoverflow.com/questions/325299/cross-platform-way-to-open-a-file-using-java-1-5
            /*if(SystemUtils.IS_OS_WINDOWS)
                Runtime.getRuntime().exec("cmd.exe /C" + file.getAbsolutePath()).waitFor();
            else if(SystemUtils.IS_OS_LINUX)
                    Runtime.getRuntime().exec("xdg-open " + file.getAbsolutePath()).waitFor();
            else if(SystemUtils.IS_OS_MAC)
                Runtime.getRuntime().exec("open " + file.getAbsolutePath()).waitFor();*/

        Desktop.getDesktop().open(file);
    }

    public static String getFileName(String name, String extension) {
        return (extension != null && !extension.isEmpty() ? (name + "." + extension) : name).replaceAll("[/\\\\]", ""); //remove / and \
    }
    public static String addExtension(String name, String extension) {
        return extension != null && !extension.isEmpty() ? (name + "." + extension) : name;
    }

    public static String firstWord(String string, String separator) {
        int first = string.indexOf(separator);
        if (first >= 0)
            return string.substring(0, first);
        else
            return string;
    }

    public static String[] monthsRussian = new String[]{"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    // приходится складывать в baseUtils, потому что должна быть единая функция и для сервера и для клиента
    // так как отчеты формируются и на сервере
    // используется в *.jrxml
    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(Date date) {
        return formatRussian(date, false, false);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(Date date, TimeZone timeZone) {
        return formatRussian(date, false, false, timeZone, false);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(Date date, TimeZone timeZone, boolean noYear) {
        return formatRussian(date, false, false, timeZone, noYear);
    }

    public static String formatRussian(Date date, boolean quotes, boolean leadZero) {
        return formatRussian(date, quotes, leadZero, null, false);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(Date date, boolean quotes, boolean leadZero, boolean noYear) {
        return formatRussian(date, quotes, leadZero, null, noYear);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(Date date, boolean quotes, boolean leadZero, TimeZone timeZone) {
        return formatRussian(date, quotes, leadZero, timeZone, false);
    }

    public static String formatRussian(Date date, boolean quotes, boolean leadZero, TimeZone timeZone, boolean noYear) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        if (timeZone != null)
            calendar.setTimeZone(timeZone);
        String dayOfMonth = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        if ((leadZero) && (dayOfMonth.length() == 1))
            dayOfMonth = "0" + dayOfMonth;
        if (quotes)
            dayOfMonth = "«" + dayOfMonth + "»";

        return "" + dayOfMonth + " " + monthsRussian[calendar.get(Calendar.MONTH)] + (noYear ? "" : (" " + calendar.get(Calendar.YEAR)));
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(LocalDate date) {
        return formatRussian(date, false, false);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatRussian(LocalDate date, boolean noYear) {
        return formatRussian(date, false, false, noYear);
    }

    public static String formatRussian(LocalDate date, boolean quotes, boolean leadZero) {
        return formatRussian(date, quotes, leadZero, false);
    }

    public static String formatRussian(LocalDate date, boolean quotes, boolean leadZero, boolean noYear) {
        String dayOfMonth = String.valueOf(date.getDayOfMonth());
        if ((leadZero) && (dayOfMonth.length() == 1))
            dayOfMonth = "0" + dayOfMonth;
        if (quotes)
            dayOfMonth = "«" + dayOfMonth + "»";

        return "" + dayOfMonth + " " + monthsRussian[date.getMonthValue() - 1] + (noYear ? "" : (" " + date.getYear()));
    }

    public static String[] monthsEnglish = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

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

    @SuppressWarnings({"UnusedDeclaration"})
    public static String formatEnglish(LocalDate date) {
        return formatEnglish(date, false, false);
    }

    public static String formatEnglish(LocalDate date, boolean quotes, boolean leadZero) {
        String dayOfMonth = String.valueOf(date.getDayOfMonth());
        if ((leadZero) && (dayOfMonth.length() == 1))
            dayOfMonth = "0" + dayOfMonth;
        if (quotes)
            dayOfMonth = "“" + dayOfMonth + "”";

        return "" + monthsEnglish[date.getMonthValue() - 1] + " " + dayOfMonth + ", " + date.getYear();
    }

    public static String getDatePattern() {
        return getSystemDateFormat(DateFormat.SHORT, true);
    }

    public static String getTimePattern() {
        return getSystemDateFormat(DateFormat.SHORT, false);
    }

    public static Locale defaultFormatLocale;
    private static String getSystemDateFormat(int style, boolean date)  {
        Locale locale = nvl(defaultFormatLocale, Locale.getDefault(Locale.Category.FORMAT));
        try {
            DateFormatProvider provider = ReflectionUtils.getMethodValue(ReflectionUtils.classForName("sun.util.locale.provider.HostLocaleProviderAdapterImpl"),
                    null, "getDateFormatProvider", new Class[]{}, new Object[]{});
            String formatPattern = ((SimpleDateFormat) (date ? provider.getDateInstance(style, locale) : provider.getTimeInstance(style, locale))).toPattern();

            //windows time format sometimes contains "aa" as AM / PM marker but DateTimeFormatter in TFormats support only one "a" letter in time / dateTime formats.
            if (formatPattern.matches(".* ?(aa)"))
                formatPattern = formatPattern.replace("aa", "a");

            return formatPattern;
        } catch(Exception e) {
            //openJDK has no getDateFormatProvider method
            return ((SimpleDateFormat) (date ? DateFormat.getDateInstance(DateFormat.SHORT, locale) : DateFormat.getTimeInstance(DateFormat.SHORT, locale))).toPattern();
        }
    }

    //date pattern allows only "d" (day), "M" (month), "y" (year) symbols and "\", "/", ".", "-", ",", ":", " " (delimiters)
    //at least one "d", "M" and "y" symbol is required
    //dateTime pattern also allow "H" (hour), "m" (minute), "s" (second), "S" (millisecond). At least one "H" and "m" symbols are required
    public static String getValidEditDateFormat(String pattern, boolean dateTime) {
        String regexp = dateTime ? "[^dMyHmsS\\s\\\\/.,\\-:]|M{3,}" : "[^dMy\\s\\\\/.,\\-:]|M{3,}";
        Stream<String> requiredSymbols = dateTime ? Stream.of("d", "M", "y", "H", "m") : Stream.of("d", "M", "y");
        pattern = pattern.replaceAll(regexp, "").trim();
        return requiredSymbols.allMatch(pattern::contains) ? pattern : null;
    }

    public static String getFileName(File file) {
        return getFileName(file.getName());
    }

    public static String getFileName(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static String getFileNameAndExtension(String filename) {
        return FilenameUtils.getName(filename);
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    public static String replaceFileName(String filePath, String toName, boolean escapeDot) {
        return FilenameUtils.getFullPath(filePath) + toName + (escapeDot ? "\\" : "") + "." + FilenameUtils.getExtension(filePath);
    }
    public static String replaceFileNameAndExtension(String filePath, String toName) {
        return FilenameUtils.getFullPath(filePath) + toName;
    }
    public static String addSuffix(String filePath, String suffix) {
        return FilenameUtils.getFullPath(filePath) + FilenameUtils.getBaseName(filePath) + suffix + "." + FilenameUtils.getExtension(filePath);
    }
    public static String addPrefix(String filePath, String prefix) {
        return FilenameUtils.getFullPath(filePath) + prefix + FilenameUtils.getBaseName(filePath) + "." + FilenameUtils.getExtension(filePath);
    }

    public static Object filesToBytes(boolean multiple, boolean storeName, boolean custom, boolean named, String namedFileName, File... files) {
        try {
            String[] namedFileNames = new String[files.length];
            String[] fileNames = new String[files.length];
            FileInputStream[] fileStreams = new FileInputStream[files.length];
            for(int i=0;i<files.length;i++) {
                namedFileNames[i] = namedFileName;
                fileNames[i] = files[i].getName();
                fileStreams[i] = new FileInputStream(files[i]);
            }
            try {
                return filesToBytes(multiple, storeName, custom, named, namedFileNames, fileNames, fileStreams);
            } finally {
                for(int i=0;i<files.length;i++)
                    fileStreams[i].close();
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
    public static Object filesToBytes(boolean multiple, boolean storeName, boolean custom, boolean named, String[] namedFileNames, String[] fileNames, InputStream[] fileStreams) {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();

        byte[] result;
        try(DataOutputStream outStream = new DataOutputStream(byteOutStream)) {
            int length = fileNames.length;
            if (multiple)
                outStream.writeInt(length);
            for (int i=0;i<length;i++) {
                String fileName = fileNames[i];
                InputStream fileStream = fileStreams[i];
                String namedFileName = namedFileNames[i];
                if (storeName) {
                    outStream.writeInt(fileName.length());
                    outStream.writeBytes(fileName);
                }
                RawFileData rawFileData = new RawFileData(fileStream);

                byte[] fileBytes;
                if (custom) {
                    String ext = getFileExtension(fileName);
                    if(named) {
                        assert !multiple && !storeName;
                        return new NamedFileData(new FileData(rawFileData, ext), getFileName(namedFileName != null ? namedFileName : fileName));
                    } else {
                        FileData fileData = new FileData(rawFileData, ext);
                        if(!(multiple || storeName))
                            return fileData;
                        fileBytes = fileData.getBytes();
                    }
                } else {
                    if(!(multiple || storeName))
                        return rawFileData;
                    fileBytes = rawFileData.getBytes();
                }

                outStream.writeInt(fileBytes.length);
                outStream.write(fileBytes);
            }

            result = byteOutStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(multiple || storeName)
            return result;
        else
            return null;
    }

    public static void safeDelete(File file) {
        if (file != null && !file.delete()) {
            file.deleteOnExit();
        }
    }

    public static int[] consecutiveInts(int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; ++i) {
            result[i] = i;
        }
        return result;
    }

    public static int max(int a, int b) {
        return a > b ? a : b;
    }

    public static double max(double a, double b) {
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

    public static long min(long a, long b) {
        return a < b ? a : b;
    }

    public static double min(double a, double b) {
        return a > b ? b : a;
    }

    public static List<Integer> consecutiveList(int length, int start) {
        List<Integer> result = new ArrayList<>();
        for (int j = 0; j < length; j++)
            result.add(j + start);
        return result;
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
        return new MergeFunctionSet<>(set1, set2);
    }

    public static <K> FunctionSet<K> mergeElement(FunctionSet<K> set1, K element) {
        if(set1.contains(element))
            return set1;
        
        return merge(set1, SetFact.singleton(element));        
    }

    public static <K> FunctionSet<K> remove(FunctionSet<K> set1, FunctionSet<K> set2) {
        if (set1.isEmpty() || set2.isFull())
            return SetFact.EMPTY();
        if (set2.isEmpty() || set1.isFull())
            return set1;
//        if(set1 instanceof ImSet && set2 instanceof ImSet)
//            return ((ImSet<K>)set1).merge((ImSet<K>)set2);
        return new RemoveFunctionSet<>(set1, set2);
    }

    public static <MK, K, V> void putUpdate(Map<MK, Map<K, V>> keyValues, MK key, Map<K, V> values, boolean update) {
        if (update)
            keyValues.put(key, BaseUtils.override(keyValues.get(key), values));
        else
            keyValues.put(key, values);
    }

    public static <V> V getNearObject(V findValue, List<V> keys) {
        if (keys.size() <= 1)
            return null;

        V nearObject = null;
        for (V key : keys) {
            if (key.equals(findValue) && nearObject == null) {
                int index = keys.indexOf(key);
                index = index == keys.size() - 1 ? index - 1 : index + 1;
                nearObject = keys.get(index);
            }
        }
        return nearObject;
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

    public static String calculateBase64Hash(String algorithm, String input, String salt) throws RuntimeException {
        return toHashString(Base64.encodeBase64(calculateHash(algorithm, input, salt)));
    }

    public static byte[] calculateHash(String algorithm, String input, String salt) throws RuntimeException {
        try {
            return MessageDigest.getInstance(algorithm).digest(mergePasswordAndSalt(input, salt).getBytes(ExternalUtils.hashCharset));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String mergePasswordAndSalt(String password, String salt) {
        if (password == null) {
            password = "";
        }
        if (isEmpty(salt)) {
            return password;
        } else {
            return password + "{" + salt + "}";
        }
    }

    public static int compareInts(int a, int b) {
        return Integer.compare(a, b);
    }

    public static String bigDecimalToString(BigDecimal bd) {
        return bigDecimalToString("#,##0.###", bd);
    }

    public static String bigDecimalToString(String format, BigDecimal bd) {
        return new DecimalFormat(format).format(bd == null ? BigDecimal.ZERO : bd);
    }

    public static String dateToString(Date d) {
        return dateToString("dd/MM/yyyy", d);
    }

    public static String dateToString(String format, Date d) {
        return new SimpleDateFormat(format).format(d);
    }

    public static String dateToString(String format, LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern(format)) : "";
    }

    public static String packWords(String string, int reqLength) {
        if (string.length() <= reqLength)
            return string;

        String[] words = string.split("(?<!^)(?=[A-Z])");
        float cut = (float) reqLength / (float) string.length();

        int[] keepLength = new int[words.length];
        int total = 0;
        for (int i = 0; i < words.length; i++) {
            int rounded = (int) (((float) words[i].length()) * cut);
            keepLength[i] = rounded;
            total += rounded;
        }

        int rest = reqLength - total;
        assert rest >= 0 && rest <= words.length;
        for (int i = 0; i < rest; i++)
            keepLength[i]++;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            result.append(words[i].substring(0, keepLength[i]));
        }
        return result.toString();
    }

    public static <T> Iterable<T> mergeIterables(final Iterable<T> it1, final Iterable<T> it2) {
        return () -> mergeIterators(it1.iterator(), it2.iterator());
    }

    public static <T> Iterator<T> mergeIterators(final Iterator<T> it1, final Iterator<T> it2) {
        return new Iterator<T>() {
            boolean it1Running = true;

            public boolean hasNext() {
                return (it1Running && it1.hasNext()) || it2.hasNext();
            }

            @Override
            public T next() {
                if (it1Running) {
                    if (it1.hasNext())
                        return it1.next();
                    else
                        it1Running = false;
                }
                return it2.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected enum Check {
        FIRST, CLOSEST, COMMON
    }

    public interface ChildrenInterface<T> {
        Iterable<T> getChildrenIt(T element);
    }

    public interface ExChildrenInterface<T> extends ChildrenInterface<T> {
        ImSet<T> getAllChildren(T element); // оптимизация, чтобы кэширование включать
    }

    public static <V> ImSet<V> getAllChildren(V vthis, ChildrenInterface<V> cint) {
        MSet<V> mChilds = SetFact.mSet();
        fillChildren(vthis, cint, mChilds);
        return mChilds.immutable();
    }

    public static <V> void fillChildren(V vthis, ChildrenInterface<V> cint, MSet<V> classSet) {
        classSet.add(vthis);

        for (V child : cint.getChildrenIt(vthis))
            fillChildren(child, cint, classSet);
    }

    // получает классы у которого есть оба интерфейса
    public static <V> ImSet<V> commonChildren(V vthis, V toCommon, ExChildrenInterface<V> cint) {
        MAddMap<V, Check> checks = commonClassSet1(vthis, cint);
        commonClassSet2(toCommon, cint, false, null, checks);

        MSet<V> mResult = SetFact.mSet();
        commonClassSet3(vthis, cint, mResult, null, checks);
        return mResult.immutable();
    }

    // 1-й шаг расставляем пометки 1 
    private static <V> MAddMap<V, Check> commonClassSet1(V vthis, ExChildrenInterface<V> cint) {
        return MapFact.mAddOverrideMap(cint.getAllChildren(vthis).toMap(Check.FIRST));
    }

    // 2-й шаг пометки
    // 2 - самый верхний \ нижний общий класс
    // 3 - остальные общие классы
    private static <V> void commonClassSet2(V vthis, ExChildrenInterface<V> cint, boolean set, MSet<V> free, MAddMap<V, Check> checks) {
        Check check = checks.get(vthis);
        if (!set) {
            if (check != null) {
                if (check != Check.FIRST) return;
                checks.add(vthis, Check.CLOSEST);
                set = true;
            } else if (free != null) free.add(vthis);
        } else {
            if (check == Check.COMMON)
                return;

            checks.add(vthis, Check.COMMON);
            if (check == Check.CLOSEST)
                return;
        }

        for (V child : cint.getChildrenIt(vthis))
            commonClassSet2(child, cint, set, free, checks);
    }

    // 3-й шаг выводит в Set, и сбрасывает пометки
    private static <V> void commonClassSet3(V vthis, ExChildrenInterface<V> cint, MSet<V> common, MSet<V> free, MAddMap<V, Check> checks) {
        Check check = checks.get(vthis);
        if (check == null) return;
        if (common != null && check == Check.CLOSEST) common.add(vthis);
        if (free != null && check == Check.FIRST) free.add(vthis);

        for (V child : cint.getChildrenIt(vthis))
            commonClassSet3(child, cint, common, free, checks);
    }

    public static <V> void fillDiffChildren(V vthis, ExChildrenInterface<V> cint, V vdiff, MSet<V> mAdd, MSet<V> mRemove) {
 
        MAddMap<V, Check> checks = commonClassSet1(vthis, cint); // check
        if(vdiff!=null) commonClassSet2(vdiff, cint, false, mRemove, checks);
    
        commonClassSet3(vthis, cint, null,mAdd, checks);
    }

    public static double pow(double value, int pow) {
        double result = value;
        for(int i=0;i<pow-1;i++)
            result = result * value;
        return result;
    }

    public static double pow(double value, long pow) {
        double result = value;
        for(int i=0;i<pow-1;i++)
            result = result * value;
        return result;
    }

    public static String trimToEmpty(String str) {
        return str == null?"":str.trim();
    }

    public static String trim(String str) {
        return str == null?null:str.trim();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static String trimToNull(String str) {
        String ts = trim(str);
        return isEmpty(ts)?null:ts;
    }

    public static String substring(String value, int length) {
        return value == null ? null : value.substring(0, Math.min(value.length(), length));
    }
    
    public static <E> int indexOf(PriorityQueue<E> queue, E element) {
        int index = 0;
        for(E qel : queue) {
            if(BaseUtils.hashEquals(qel, element))
                index++;
        }
        return index;
    }
    
    public static String defaultToString(Object o) {
        return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String generatePassword(int length, boolean useAtLeastOneDigit, boolean useBothRegisters) {
        String password = null;
        while (password == null || (useAtLeastOneDigit && !password.matches(".*\\d.*")) || (useBothRegisters && (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*")))) {
            StringBuilder passwordBuilder = new StringBuilder(length);
            Random random = new Random(System.nanoTime());

            for (int i = 0; i < length; i++) {
                passwordBuilder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            password = passwordBuilder.toString();
        }
        return password;
    }

    public static final String impossibleString = "FDWREVSFFGFSDRSDR";

    public static final String inlineFileSeparator = "<PQWERJUQMASPRETQT/>"; // we want separators as tags to have no problem with ts vectors

    public static final String inlineImageSeparator = "<GFDTRGDFSAFADXZW/>";

    public static final String inlineSerializedImageSeparator = "<DFSRKNFDVSDRRES/>";

    public static Object executeWithTimeout(Callable<Object> callable, Integer timeout) {
        if (timeout != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            final Future<Object> future = executor.submit(callable);
            executor.shutdown();

            try {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                future.cancel(true);
                throw Throwables.propagate(e);
            }
        } else {
            try {
                return callable.call();
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    public static void writeObject(DataOutputStream outStream, Object object) throws IOException {
        outStream.writeBoolean(object != null);
        if (object != null) {
            new ObjectOutputStream(outStream).writeObject(object);
        }
    }

    public static <T> T readObject(DataInputStream inStream) throws IOException {
        try {
            if (inStream.readBoolean()) {
                T object = (T) new ObjectInputStream(inStream).readObject();
                return object;
            } else {
                return null;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException(getString("serialization.can.not.read.object"), e);
        }
    }

    public static <T> java.util.function.Predicate<T> or(java.util.function.Predicate<T> a, java.util.function.Predicate<T> b) {
        return value -> a.test(value) || b.test(value);
    }

    public static boolean endsWithIgnoreCase(String s, String suffix) {
        return s != null && suffix != null && s.toLowerCase().endsWith(suffix.toLowerCase());
    }

    public static String[] getNotNullStringArray(String[] array) {
        return array == null ? new String[0] : array;
    }

    public static int roundToDegree(int base, int value) {
        return (int) (Math.pow(base, Math.log(value) / Math.log(base)));
    }
}

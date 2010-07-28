package platform.base;

import java.io.*;
import java.util.*;

public class BaseUtils {

    public static boolean nullEquals(Object obj1, Object obj2) {

        if (obj1 == null)
            return obj2 == null;
        else
            return obj1.equals(obj2);
    }

    public static int nullHash(Object obj) {
        return obj==null?0:obj.hashCode();
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

    public static <K,E,V> Map<K,V> nullJoin(Map<K,? extends E> map, Map<E,V> joinMap) {
        return joinMap==null?null:join(map, joinMap);
    }

    public static <K,E,V> Map<K,V> join(Map<K,? extends E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,? extends E> entry : map.entrySet())
            result.put(entry.getKey(),joinMap.get(entry.getValue()));
        return result;
    }

    public static <K,V> List<V> mapList(List<? extends K> list, Map<K,V> map) {
        List<V> result = new ArrayList<V>();
        for(K element : list)
            result.add(map.get(element));
        return result;
    }

    public static <K,E,V> Map<K,V> innerJoin(Map<K,E> map, Map<E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,E> entry : map.entrySet()) {
            V joinValue = joinMap.get(entry.getValue());
            if(joinValue!=null) result.put(entry.getKey(), joinValue);
        }
        return result;
    }

    public static <K,V> Map<K,V> filterValues(Map<K,V> map, Collection<V> values) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,V> entry : map.entrySet())
            if(values.contains(entry.getValue()))
                result.put(entry.getKey(),entry.getValue());
        return result;
    }

    // необходимо чтобы пересоздавал объект !!! потому как на вход идут mutable'ы
    public static <K,V> Collection<K> filterValues(Map<K,V> map, V value) {
        Collection<K> result = new ArrayList<K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            if(value.equals(entry.getValue()))
                result.add(entry.getKey());
        return result;
    }


    public static <BK,K extends BK,V> Map<K,V> filterKeys(Map<BK,V> map, Collection<K> keys) {
        Map<K,V> result = new HashMap<K, V>();
        for(K key : keys) {
            V value = map.get(key);
            if(value!=null) result.put(key,value);
        }
        return result;
    }

    public static <K,V> Map<K,V> filterNotKeys(Map<K,V> map, Collection<? extends K> keys) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,V> entry : map.entrySet()) {
            if(!keys.contains(entry.getKey()))
                result.put(entry.getKey(),entry.getValue());
        }
        return result;
    }

    public static <BK,K extends BK,V> Map<K,V> splitKeys(Map<BK,V> map, Collection<K> keys, Map<BK,V> rest) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<BK,V> entry : map.entrySet())
            if(keys.contains(entry.getKey()))
                result.put((K) entry.getKey(),entry.getValue());
            else
                rest.put(entry.getKey(),entry.getValue());
        return result;
    }

    public static <BV,V extends BV,K> Map<K,V> splitValues(Map<K,BV> map, Collection<V> keys, Map<K,BV> rest) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,BV> entry : map.entrySet())
            if(keys.contains(entry.getValue()))
                result.put(entry.getKey(), (V) entry.getValue());
            else
                rest.put(entry.getKey(),entry.getValue());
        return result;
    }

    public static <K,V> Map<V,K> reverse(Map<K,V> map) {
        Map<V,K> result = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet()) {
            assert !result.containsKey(entry.getValue());
            result.put(entry.getValue(),entry.getKey());
        }
        return result;
    }

    public static <K,VA,VB> Map<VA,VB> crossJoin(Map<K,VA> map,Map<K,VB> mapTo) {
        return join(reverse(map),mapTo);
    }

    public static <KA,VA,KB,VB> Map<VA,VB> crossJoin(Map<KA,VA> map,Map<KB,VB> mapTo,Map<KA,KB> mapJoin) {
        return join(crossJoin(map,mapJoin),mapTo);
    }

    public static <KA,KB,V> Map<KA,KB> crossValues(Map<KA,V> map,Map<KB,V> mapTo) {
        return join(map,reverse(mapTo));
    }

    public static <K> Collection<K> join(Collection<K> col1, Collection<K> col2) {
        Set<K> result = new HashSet<K>(col1);
        result.addAll(col2);
        return result;
    }

    public static <K,V> boolean identity(Map<K,V> map) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(!entry.getKey().equals(entry.getValue())) return false;
        return true;
    }

    public static <K> Map<K,K> toMap(Set<K> collection) {
        Map<K,K> result = new HashMap<K, K>();
        for(K object : collection)
            result.put(object,object);
        return result;
    }

    public static <K> Map<Integer,K> toMap(List<K> list) {
        Map<Integer,K> result = new HashMap<Integer, K>();
        for(int i=0;i<list.size();i++)
            result.put(i,list.get(i));
        return result;
    }

    public static <K> Map<Integer,K> toMap(K[] list) {
        Map<Integer,K> result = new HashMap<Integer, K>();
        for(int i=0;i<list.length;i++)
            result.put(i,list[i]);
        return result;
    }

    public static <K> List<K> toList(Map<Integer,K> map) {
        List<K> result = new ArrayList<K>();
        for(int i=0;i<map.size();i++)
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

        if (objectType == 6) {
            return new java.sql.Date(inStream.readLong());
        }

        if (objectType == 7) {
            int len = inStream.readInt();
            return IOUtils.readBytesFromStream(inStream, len);
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

        if (object instanceof java.sql.Date) {
            outStream.writeByte(6);
            outStream.writeLong(((java.sql.Date)object).getTime());
            return;
        }

        if (object instanceof byte[]) {
            byte[] obj = (byte[]) object;
            outStream.writeByte(7);
            outStream.writeInt(obj.length);
            outStream.write(obj);
            return;
        }

        throw new IOException();
    }// -------------------------------------- Сериализация классов -------------------------------------------- //

    public static byte[] serializeObject(Object value) throws IOException {

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        serializeObject(new DataOutputStream(outStream), value);
        return outStream.toByteArray();
    }


    public static boolean startsWith(char[] string,int off,char[] check) {
        if(string.length-off<check.length)
            return false;

        for(int i=0;i<check.length;i++)
            if(string[off+i]!=check[i])
                return false;
        return true;
    }

    public static <K,V> Map<K,V> removeKeys(Map<K,V> map,Collection<K> remove) {
        Map<K,V> removeMap = new HashMap<K,V>();
        for(Map.Entry<K,V> property : map.entrySet())
            if(!remove.contains(property.getKey()))
                removeMap.put(property.getKey(),property.getValue());
        return removeMap;
    }

    public static <K,V> Map<K,V> removeKey(Map<K,V> map,K remove) {
        Map<K,V> removeMap = new HashMap<K,V>();
        for(Map.Entry<K,V> property : map.entrySet())
            if(!property.getKey().equals(remove))
                removeMap.put(property.getKey(),property.getValue());
        return removeMap;
    }

    public static <K> Collection<K> add(Collection<K> col,K add) {
        Collection<K> result = new ArrayList<K>(col);
        result.add(add);
        return result;
    }

    public static <K> Collection<K> remove(Collection<? extends K> set,Collection<? extends K> remove) {
        Collection<K> result = new ArrayList<K>(set);
        result.removeAll(remove);
        return result;
    }

    public static <K> Set<K> removeSet(Set<? extends K> set,Set<? extends K> remove) {
        Set<K> result = new HashSet<K>(set);
        result.removeAll(remove);
        return result;
    }

    public static <K> List<K> removeList(List<K> list,Collection<K> remove) {
        List<K> removeList = new ArrayList<K>();
        for(K property : list)
            if(!remove.contains(property))
                removeList.add(property);
        return removeList;
    }

    public static int max(int a,int b) {
        return a>b?a:b;
    }
    public static int min(int a,int b) {
        return a<b?a:b;
    }

    public static <K> Set<K> toSet(Collection<K> collection) {
        Set<K> set = new HashSet<K>();
        for(K element : collection)
            set.add(element);
        return set;
    }

    public static <B,K1 extends B,K2 extends B,V> Map<B,V> merge(Map<K1,? extends V> map1,Map<K2,? extends V> map2) {
        Map<B,V> result = new HashMap<B,V>(map1);
        result.putAll(map2);
        return result;
    }

    public static <B,K1 extends B,K2 extends B,V> Map<B,V> mergeEqual(Map<K1,? extends V> map1,Map<K2,? extends V> map2) {
        Set<? extends V> values1 = BaseUtils.reverse(map1).keySet();
        Map<B,V> result = new HashMap<B,V>(map1);
        for(Map.Entry<K2,? extends V> entry2 : map2.entrySet()) {
            V value1 = result.put(entry2.getKey(), entry2.getValue());
            if(value1==null) {
                if(values1.contains(entry2.getValue())) // повторяется значение - не reversed
                    return null;
            } else
                if(!value1.equals(entry2.getValue())) // повторяется ключ
                    return null;
        }
        return result;
    }

    public static <K,V> boolean isSubMap(Map<? extends K,? extends V> map1,Map<K,? extends V> map2) {
        for(Map.Entry<? extends K,? extends V> entry : map1.entrySet()) {
            V value2 = map2.get(entry.getKey());
            if(!(value2!=null && hashEquals(value2,entry.getValue())))
                return false;
        }
        return true;
    }

    public static <B,V> Map<B,V> forceMerge(Map<?,? extends V> map1,Map<?,? extends V> map2) {
        Map<Object,V> result = new HashMap<Object,V>(map1);
        result.putAll(map2);
        return (Map<B,V>)result;
    }

    public static <B,K1 extends B,K2 extends B> Collection<B> merge(Collection<K1> col1,Collection<K2> col2) {
        Collection<B> result = new ArrayList<B>(col1);
        result.addAll(col2);
        return result;
    }

    public static <B,K1 extends B,K2 extends B> Set<B> mergeSet(Set<K1> set1,Set<K2> set2) {
        Set<B> result = new HashSet<B>(set1);
        result.addAll(set2);
        return result;
    }

    public static <B,K1 extends B,K2 extends B> Set<B> mergeItem(Set<K1> set,K2 item) {
        Set<B> result = new HashSet<B>(set);
        result.add(item);
        return result;
    }

    public static <B,K1 extends B,K2 extends B> List<B> mergeList(List<K1> list1,List<K2> list2) {
        List<B> result = new ArrayList<B>(list1);
        result.addAll(list2);
        return result;
    }

    public static <V,MV,EV> Map<Object,EV> mergeMaps(Map<V, EV> map, Map<MV, EV> toMerge,Map<MV, Object> mergedMap) {
        Map<Object,EV> merged = new HashMap<Object,EV>(map);
        Map<EV,Object> reversed = BaseUtils.reverse(merged);
        for(Map.Entry<MV,EV> transEntry : toMerge.entrySet()) {
            Object mergedProp = reversed.get(transEntry.getValue());
            if(mergedProp==null) {
                mergedProp = new Object();
                merged.put(mergedProp, transEntry.getValue());
            }
            mergedMap.put(transEntry.getKey(),mergedProp);
        }
        return merged;
    }

    // ищет в Map рекурсивно тупик
    public static <K> K findDeadEnd(Map<K,K> map,K end) {
        K next = map.get(end);
        if(next==null)
            return end;
        else
            return findDeadEnd(map,end);
    }

    public static <T> boolean equalArraySets(T[] array1,T[] array2) {
        if(array1.length!=array2.length) return false;
        T[] check2 = array2.clone();
        for(T element : array1) {
            boolean found = false;
            for(int i=0;i<check2.length;i++)
                if(check2[i]!=null && BaseUtils.hashEquals(element,check2[i])) {
                    check2[i] = null;
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    public static <T> int hashArraySet(T[] array) {
        int hash = 0;
        for(T element : array)
            hash += element.hashCode();
        return hash;
    }


    public static <T> T nvl(T value1,T value2) {
        return value1==null?value2:value1;
    }

    public static String evl(String primary,String secondary) {
        return (primary.length()==0?secondary:primary);
    }

    public static boolean hashEquals(Object obj1,Object obj2) {
        return obj1==obj2 || (obj1.hashCode()==obj2.hashCode() && obj1.equals(obj2));
    }

    public static <T> boolean contains(T[] array,T element) {
        return contains(array, element, array.length);
    }
    public static <T> boolean contains(T[] array,T element,int num) {
        for(int i=0;i<num;i++)
            if(array[i].equals(element))
                return true;
        return false;
    }

    public static <T> Set<T> toSet(T[] array) {
        return new HashSet<T>(Arrays.asList(array));
    }

    public static <T> T getRandom(List<T> list,Random randomizer) {
        return list.get(randomizer.nextInt(list.size()));
    }

    public static String clause(String clause,String data) {
        return (data.length()==0?"":" "+ clause +" "+ data);
    }

    static String clause(String clause,int data) {
        return (data ==0?"":" "+ clause +" "+ data);
    }

    public static <T, K> OrderedMap<T, K> listMap(List<T> list, Map<T, K> map) {
        OrderedMap<T, K> result = new OrderedMap<T, K>();
        for (T element : list)
            result.put(element, map.get(element));
        return result;
    }


    public static abstract class Group<G,K> {
        public abstract G group(K key);
    }
    public static <G,K> Map<G,Collection<K>> group(Group<G, K> getter, Collection<K> keys) {
        Map<G,Collection<K>> result = new HashMap<G, Collection<K>>();        
        for(K key : keys) {
            G group = getter.group(key);
            Collection<K> groupList = result.get(group);
            if(groupList==null) {
                groupList = new ArrayList<K>();
                result.put(group,groupList);
            }
            groupList.add(key);
        }
        return result;
    }

    public static <V> V addValue(Map<V,V> values,V value) {
        V addValue = values.get(value); // смотрим может уже есть
        if(addValue==null) { // если нету, находим рекурсивно первое свободное значение
            addValue = BaseUtils.findDeadEnd(BaseUtils.reverse(values), value);
            values.put(value,addValue);
        }
        return addValue;
    }

    public static <K,V> void putNotNull(K key, Map<K,V> from, Map<K,V> to) {
        V value = from.get(key);
        if(value!=null) to.put(key,value);
    }

    public static class Paired<T> {
        public final T[] common;

        private final T[] diff1;
        private final T[] diff2;
        private final boolean invert;

        public T[] getDiff1() { return invert?diff2:diff1; }
        public T[] getDiff2() { return invert?diff1:diff2; }

        public Paired(T[] array1,T[] array2, ArrayInstancer<T> instancer) {
            if(array1.length>array2.length) {
                T[] sw = array2; array2 = array1; array1 = sw; invert = true;
            } else
                invert = false;
            assert array1.length<=array2.length;
            T[] pairedWheres = instancer.newArray(array1.length); int pairs = 0;
            T[] thisWheres = instancer.newArray(array1.length); int thisnum = 0;
            T[] pairedThatWheres = array2.clone();
            for(T opWhere : array1) {
                boolean paired = false;
                for(int i=0;i<pairedThatWheres.length;i++)
                    if(pairedThatWheres[i]!=null && hashEquals(array2[i],opWhere)) {
                        pairedWheres[pairs++] = opWhere;
                        pairedThatWheres[i] = null;
                        paired = true;
                        break;
                    }
                if(!paired) thisWheres[thisnum++] = opWhere;
            }

            if(pairs==0) {
                common = instancer.newArray(0);
                diff1 = array1;
                diff2 = array2;
            } else {
                if(pairs==array1.length) {
                    common = array1;
                    diff1 = instancer.newArray(0);
                } else {
                    common = instancer.newArray(pairs); System.arraycopy(pairedWheres,0,common,0,pairs);
                    diff1 = instancer.newArray(thisnum); System.arraycopy(thisWheres,0,diff1,0,thisnum);
                }

                if(pairs==array2.length)
                    diff2 = diff1;
                else {
                    diff2 = instancer.newArray(array2.length-pairs); int compiledNum = 0;
                    for(T opWhere : pairedThatWheres)
                        if(opWhere!=null) diff2[compiledNum++] = opWhere;
                }
            }
        }
    }

    public static <K,V> boolean hashContainsValue(Map<K,V> map, V value) {
        for(Map.Entry<K,V> entry : map.entrySet())
            if(hashEquals(entry.getValue(),value))
                return true;
        return false;
    }

    public static <K,EK,T> Map<K, EK> mapEquals(Map<K,T> map, Map<EK,T> equals) {
        if(map.size()!=equals.size()) return null;

        Map<K, EK> mapKeys = new HashMap<K, EK>();
        for(Map.Entry<K,T> key : map.entrySet()) {
            EK mapKey = null;
            for(Map.Entry<EK,T> equalKey : equals.entrySet())
                if(!hashContainsValue(mapKeys,equalKey.getKey()) &&
                    hashEquals(key.getValue(),equalKey.getValue())) {
                    mapKey = equalKey.getKey();
                    break;
                }
            if(mapKey==null) return null;
            mapKeys.put(key.getKey(),mapKey);
        }
        return mapKeys;
    }

    public static <K> String toString(Collection<K> array, String separator) {
        String result = "";
        for(K element : array)
            result = (result.length()==0?"":result+separator) + element;
        return result;
    }
    public static <K> String toString(K[] array, String separator) {
        String result = "";
        for(K element : array)
            result = (result.length()==0?"":result+separator) + element;
        return result;
    }

    public static <K,V> boolean containsAll(Map<K,V> map, Map<K,V> contains) {
        for(Map.Entry<K,V> entry : contains.entrySet())
            if(!entry.getValue().equals(map.get(entry.getKey())))
                return false;
        return true;
    }

    public static <K> Map<K,String> mapString(Collection<K> col) {
        Map<K,String> result = new HashMap<K, String>();
        for(K element : col)
            result.put(element,element.toString());
        return result;
    }

    public static Object[] add(Object[] array1,Object[] array2) {
        return add(array1,array2,objectInstancer);
    }

    public static Object[] add(List<Object[]> list) {
        int totLength = 0;
        for(Object[] array : list)
            totLength += array.length;
        Object[] result = new Object[totLength];
        int off = 0;
        for(Object[] array : list)
            for(Object object : array)
                result[off++] = object;
        return result;        
    }

    public final static ArrayInstancer<Object> objectInstancer = new ArrayInstancer<Object>() {
        public Object[] newArray(int size) {
            return new Object[size];
        }
    };

    public static <T> T[] add(T[] array1,T[] array2,ArrayInstancer<T> instancer) {
        T[] result = instancer.newArray(array1.length+array2.length);
        System.arraycopy(array1,0,result,0,array1.length);
        System.arraycopy(array2,0,result,array1.length,array2.length);
        return result;
    }

    public static boolean isData(Object object) {
        return object instanceof Number || object instanceof String || object instanceof Boolean || object instanceof byte[];
    }

    public static <I,E extends I> List<E> immutableCast(List<I> list) {
        return (List<E>)(List<? extends I>)list;        
    }

    public static <K,I,E extends I> Map<K,E> immutableCast(Map<K,I> map) {
        return (Map<K,E>)(Map<K,? extends I>)map;        
    }

    public static <I> I single(Collection<I> col) {
        assert col.size()==1;
        return col.iterator().next();
    }

    public static <I> I singleKey(Map<I,?> map) {
        return BaseUtils.single(map.keySet());
    }

    public static <I> I singleValue(Map<?,I> map) {
        return BaseUtils.single(map.values());
    }

    public static <K,I> Map.Entry<K,I> singleEntry(Map<K,I> map) {
        return BaseUtils.single(map.entrySet());
    }

    private static <K> void reverse(Iterator<K> i, List<K> result) {
        if(i.hasNext()) {
            K item = i.next();
            reverse(i,result);
            result.add(item);
        }
    }

    public static <K> List<K> reverse(List<K> col) {
        List<K> result = new ArrayList<K>();
        reverse(col.iterator(),result);
        return result;
    }

    public static int objectToInt(Integer value) {
        if(value==null)
            return -1;
        else
            return value;
    }

    public static Integer intToObject(int value) {
        if(value==-1)
            return null;
        else
            return value;
    }

    public static String nullToString(Object name) {
        if(name==null)
            return "";
        else
            return name.toString();
    }

    public static <K,V> Map<K,V> buildMap(Collection<K> col1, Collection<V> col2) {
        assert col1.size() == col2.size();

        Iterator<K> it1 = col1.iterator();
        Iterator<V> it2 = col2.iterator();
        Map<K,V> result = new HashMap<K, V>();
        while(it1.hasNext())
            result.put(it1.next(),it2.next());
        return result;
    }

    public static <K> List<K> toList(K... elements) {
        List<K> list = new ArrayList<K>();
        Collections.addAll(list,elements);
        return list;
    }

    public static <K> Set<K> toSetElements(K... elements) {
        Set<K> set = new HashSet<K>();
        Collections.addAll(set,elements);
        return set;
    }
    
    public static String replicate(char character, int length) {

        char[] chars = new char[length];
        Arrays.fill(chars, character);
        return new String(chars);
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%1$#" + n + "s", s);
    }
}

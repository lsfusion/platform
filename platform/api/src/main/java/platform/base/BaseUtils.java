package platform.base;

import java.io.*;
import java.util.*;
import java.math.BigDecimal;

public class BaseUtils {

    public static boolean nullEquals(Object obj1, Object obj2) {

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

    public static <K,E,V> Map<K,V> join(Map<K,? extends E> map, Map<? extends E,V> joinMap) {
        Map<K,V> result = new HashMap<K, V>();
        for(Map.Entry<K,? extends E> entry : map.entrySet())
            result.put(entry.getKey(),joinMap.get(entry.getValue()));
        return result;
    }

    public static <K,E,V> LinkedHashMap<V,E> linkedJoin(LinkedHashMap<K,E> map, Map<K,V> joinMap) {
        LinkedHashMap<V,E> result = new LinkedHashMap<V, E>();
        for(Map.Entry<K,E> entry : map.entrySet())
            result.put(joinMap.get(entry.getKey()),entry.getValue());
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

    public static <K,V> Map<V,K> reverse(Map<K,V> map) {
        Map<V,K> result = new HashMap<V, K>();
        for(Map.Entry<K,V> entry : map.entrySet())
            result.put(entry.getValue(),entry.getKey());
        return result;
    }

    public static <K,VA,VB> Map<VA,VB> crossJoin(Map<K,VA> map,Map<K,VB> mapTo) {
        return join(reverse(map),mapTo);
    }

    public static <KA,VA,KB,VB> Map<VA,VB> crossJoin(Map<KA,VA> map,Map<KB,VB> mapTo,Map<KA,KB> mapJoin) {
        return join(crossJoin(map,mapJoin),mapTo);
    }

    public static <KA,VA,KB,VB> Map<KA,KB> crossValues(Map<KA,VA> map,Map<KB,VB> mapTo,Map<VA,VB> mapJoin) {
        return join(join(map,mapJoin),reverse(mapTo));
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

    public static <K> Collection<K> remove(Collection<K> set,Collection<K> remove) {
        Collection<K> result = new ArrayList<K>(set);
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
        Map<B,V> result = new HashMap<B,V>(map1);
        for(Map.Entry<K2,? extends V> entry2 : map2.entrySet()) {
            V value1 = result.put(entry2.getKey(), entry2.getValue());
            if(value1!=null && !entry2.equals(value1))
                return null;
        }
        return result;
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
                if(check2[i]!=null && element.hashCode()==check2[i].hashCode() && element.equals(check2[i])) {
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

    public static boolean hashEquals(Object obj1,Object obj2) {
        return obj1.hashCode()==obj2.hashCode() && obj1.equals(obj2);
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

    public static <K,V> void putNotNull(K key, V value, Map<K,V> map) {
        if(value!=null) map.put(key,value);
    }
}

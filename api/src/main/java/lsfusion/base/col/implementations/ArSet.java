package lsfusion.base.col.implementations;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.implementations.order.ArOrderSet;
import lsfusion.base.col.implementations.stored.StoredArSet;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.LIMIT;
import static lsfusion.base.col.implementations.stored.StoredImplementationsPolicy.STORED_FLAG;

public class ArSet<K> extends AMSet<K> {

    private int size;
    private Object[] array;

    public ArSet() {
        this.array = new Object[4];
    }

    public ArSet(int size, Object[] array) {
        this.size = size;
        this.array = array;
    }

    public ArSet(int size) {
        array = new Object[size];
    }

    public ArSet(ArSet<K> set) {
        if (!set.isStored()) {
            if (needSwitchToStored(set)) {
                switchToStored(set.size, set.array);
            } else {
                size = set.size;
                array = set.array.clone();
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public int size() {
        if (!isStored()) {
            return size;
        } else {
            return stored().size();
        }
    }

    public K get(int i) {
        if (!isStored()) {
            return (K) array[i];
        } else {
            return stored().get(i);
        }
    }

    public <M> ImValueMap<K, M> mapItValues() {
        if (!isStored()) {
            return new ArMap<>(this);
        } else {
            return stored().mapItValues();
        }
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        if (!isStored()) {
            return new ArMap<>(this);
        } else {
            return stored().mapItRevValues();
        }
    }

    @Override
    public void exclAdd(K key) { // не проверяем, чтобы в профайлере не мусорить
//        assert !contains(key);
        if (!isStored()) {
            if (size >= array.length) resize(2 * array.length);
            array[size++] = key;
            switchToStoredIfNeeded(size-1, size);
        } else {
            stored().exclAdd(key);
        }
    }
    
    public boolean add(K element) {
        if (!isStored()) {
            for (int i = 0; i < size; i++)
                if (BaseUtils.hashEquals(array[i], element))
                    return true;
            exclAdd(element);
            return false;
        } else {
            return stored().add(element);
        }
    }

    public ImSet<K> immutableCopy() {
        if (!isStored()) {
            return new ArSet<>(this);
        } else {
            return stored().immutableCopy();
        }
    }

    /*
    @Override
    public ImList<K> subList(int i, int length) {
        if(length==size()) {
            assert i==0;
            return this;
        }
        Object[] subArray = new Object[length];
        System.arraycopy(array, i, subArray, 0, length);
        return null; //new ArSet<K>(subArray);
    }*/

    private void resize(int length) {
        Object[] newArray = new Object[length];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }
    
    public static void sortArray(int size, Object[] array) {
        sortArray(size, array, (int[])null);
    }

    public static void sortArray(int size, Object[] keys, Object[] values) {
        sortArray(size, keys, values, null);
    }

    public static void sortArray(int size, Object[] keys, int[] order) {
        sortArray(size, keys, null, order);
    }

    public static void sortArray(int size, Object[] keys, Object[] values, int[] order) {
        int[] mapOrder = null;
        if(order!=null) {
            mapOrder = new int[size];
            for(int i=0;i<size;i++)
                mapOrder[i] = i;
        }

        // пузырек
/*        for (int i=0; i<size; i++) {
            int minKey = i;
            int minHash = keys[i].hashCode();
            for (int j=i+1; j<size; j++) {
                int jHash = keys[j].hashCode();
                if(jHash < minHash) {
                    minKey = j;
                    minHash = jHash;
                }
            }

            if(minKey != i) {
                Object tmp = keys[minKey];
                keys[minKey] = keys[i];
                keys[i] = tmp;
                if(values!=null) {
                    tmp = values[minKey];
                    values[minKey] = values[i];
                    values[i] = tmp;
                }
                if(order!=null) {
                    int tmpInt = mapOrder[minKey];
                    mapOrder[minKey] = mapOrder[i];
                    mapOrder[i] = tmpInt;
                }
            }
        }*/

        int[] hashes = new int[size];
        for(int i=0;i<size;i++)
            hashes[i] = keys[i].hashCode();
        sort1(hashes, 0, size, keys, values, mapOrder);

        if(order!=null) {
            for(int i=0;i<size;i++)
                order[mapOrder[i]] = i;
        }

    }

    /**
     * Returns the index of the median of the three indexed longs.
     */
    private static int med3(int x[], int a, int b, int c) {
        return (x[a] < x[b] ?
                (x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
                (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }

    private static void sort1(int x[], int off, int len, Object[] ma, Object[] em, int[] im) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i=off; i<len+off; i++)
                for (int j=i; j>off && x[j-1]>x[j]; j--)
                    swap(x, j, j-1, ma, em, im);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1);       // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) {        // Big arrays, pseudomedian of 9
                int s = len/8;
                l = med3(x, l,     l+s, l+2*s);
                m = med3(x, m-s,   m,   m+s);
                n = med3(x, n-2*s, n-s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        int v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v)
                    swap(x, a++, b, ma, em, im);
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v)
                    swap(x, c, d--, ma, em, im);
                c--;
            }
            if (b > c)
                break;
            swap(x, b++, c--, ma, em, im);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a-off, b-a  );  vecswap(x, off, b-s, s, ma, em, im);
        s = Math.min(d-c,   n-d-1);  vecswap(x, b,   n-s, s, ma, em, im);

        // Recursively sort non-partition-elements
        if ((s = b-a) > 1)
            sort1(x, off, s, ma, em, im);
        if ((s = d-c) > 1)
            sort1(x, n-s, s, ma, em, im);
    }

    private static void vecswap(int x[], int a, int b, int n, Object[] ma, Object[] em, int[] im) {
        for (int i=0; i<n; i++, a++, b++)
            swap(x, a, b, ma, em, im);
    }

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(int x[], int a, int b, Object[] ma, Object[] em, int[] im) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;

        Object tmp = ma[a];
        ma[a] = ma[b];
        ma[b] = tmp;
        if(em!=null) {
            tmp = em[a];
            em[a] = em[b];
            em[b] = tmp;
        }
        if(im!=null) {
            t = im[a];
            im[a] = im[b];
            im[b] = t;
        }
    }

    private final static int GENORDERS = 20;
    private static final int[][] genOrders = new int[GENORDERS][];
    public static int[] genOrder(int size) {
        int[] result;
        if(size >= GENORDERS || (result=genOrders[size])==null) {
            result = new int[size];
            for(int i=0;i<size;i++)
                result[i] = i;
            if(size < GENORDERS)
                genOrders[size] = result;
        }
        return result;
    }

    public ImSet<K> immutable() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if(array.length > size * SetFact.factorNotResize) {
            Object[] newArray = new Object[size];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }

        if(size < SetFact.useArrayMax)
            return this;

        // упорядочиваем Set
        sortArray(size, array);
        return new ArIndexedSet<>(size, array);
    }

    @Override
    public ArMap<K, K> toMap() {
        if (!isStored()) {
            return new ArMap<>(size, array, array);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ImRevMap<K, K> toRevMap() {
        return toMap();
    }

    @Override
    public ImOrderSet<K> toOrderSet() {
        if (!isStored()) {
            return new ArOrderSet<>(this);
        } else {
            return stored().toOrderSet();
        }
    }

    public Object[] getArray() {
        return array;
    }

    public void setArray(Object[] array) {
        this.array = array;
    }
    
    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArSet<?> set = (ArSet<?>) o;
        serializer.serialize(set.size, outStream);
        ArCol.serializeArray(set.array, serializer, outStream);    
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int)serializer.deserialize(inStream);
        Object[] array = ArCol.deserializeArray(inStream, serializer);
        return new ArSet<>(size, array);
    }

    public boolean isStored() {
        return size == STORED_FLAG;
    }

    private StoredArSet<K> stored() {
        return (StoredArSet<K>) array[0];
    }

    private void switchToStoredIfNeeded(int oldSize, int newSize) {
        if (oldSize <= LIMIT && newSize > LIMIT && needSwitchToStored(this)) {
            switchToStored(size, array);
        }
    }

    private static boolean needSwitchToStored(ArSet<?> set) {
        return !set.isStored() && set.size() > LIMIT 
                && StoredArraySerializer.getInstance().canBeSerialized(set.get(0));
    }

    private void switchToStored(int size, Object[] array) {
        StoredArSet<K> storedSet = new StoredArSet<>(StoredArraySerializer.getInstance(), size, (K[]) array);
        this.array = new Object[]{storedSet};
        this.size = STORED_FLAG;
    }
}

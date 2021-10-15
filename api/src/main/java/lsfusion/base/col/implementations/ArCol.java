package lsfusion.base.col.implementations;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.ACol;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ArCol<K> extends ACol<K> implements MCol<K>, MAddCol<K> {

    public int size;
    public Object[] array;

    public ArCol() {
        this.array = new Object[4];
    }

    public ArCol(int size) {
        this.array = new Object[size];
    }

    public ArCol(int size, Object[] array) {
        this.size = size;
        this.array = array;
    }

    public ArCol(ArCol<K> col) {
        this.size = col.size();
        this.array = col.array.clone();
    }

    public int size() {
        return size;
    }

    public K get(int i) {
        return (K) array[i];
    }

    public void set(int i, K key) {
        array[i] = key;
    }

    public void add(K key) {
        if(size>=array.length) resize(2 * array.length + 1);
        array[size++] = key;
    }

    public void addFirst(K key) {
        if(size>=array.length) resize(2 * array.length + 1);
        System.arraycopy(array, 0, array, 1, size++);
        array[0] = key;
    }

    public void remove(int i) {
        System.arraycopy(array, i + 1, array, i, size-1-i);
        array[(size--) - 1] = null;
    }

    private void resize(int length) {
        Object[] newArray = new Object[length];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }

    public void addAll(ImCol<? extends K> col) {
        for(int i=0,size=col.size();i<size;i++)
            add(col.get(i));
    }

    public ImCol<K> immutableCol() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if(array.length > size * SetFact.factorNotResize) {
            Object[] newArray = new Object[size];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }
        return this;
    }

    public Iterable<K> it() {
        return this;
    }
    
    public void removeAll() {
        size = 0;
        array = new Object[4];
    }

    public void removeLast() {
        size--;
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArCol<?> col = (ArCol<?>) o;
        serializer.serialize(col.size, outStream);
        serializeArray(col.array, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int)serializer.deserialize(inStream);
        Object[] array = deserializeArray(inStream, serializer);
        return new ArCol<>(size, array);
    }
    
    public static void serializeArray(Object[] array, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        serializer.serialize(array.length, outStream);
        for (Object obj : array) {
            serializer.serialize(obj, outStream);
        }
    } 
    
    public static Object[] deserializeArray(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int len = (int)serializer.deserialize(inStream);
        Object[] array = new Object[len];
        for (int i = 0; i < len; ++i) {
            array[i] = serializer.deserialize(inStream);
        }
        return array;
    }
    
    public static void serializeIntArray(int[] array, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        serializer.serialize(array.length, outStream);
        for (int num : array) {
            serializer.serialize(num, outStream);
        }
    }
    
    public static int[] deserializeIntArray(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int len = (int)serializer.deserialize(inStream);
        int[] array = new int[len];
        for (int i = 0; i < len; ++i) {
            array[i] = (int) serializer.deserialize(inStream);
        }
        return array;
    }
}

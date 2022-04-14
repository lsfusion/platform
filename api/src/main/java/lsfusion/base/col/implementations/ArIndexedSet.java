package lsfusion.base.col.implementations;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.implementations.order.ArOrderIndexedSet;
import lsfusion.base.col.implementations.stored.StoredArIndexedSet;
import lsfusion.base.col.implementations.stored.StoredArray;
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

public class ArIndexedSet<K> extends AMSet<K> {

    private int size;
    private Object[] array;

    public ArIndexedSet() {
        this.array = new Object[4];
    }

    public ArIndexedSet(int size, Object[] array) {
        this.size = size;
        this.array = array;
    }

    public ArIndexedSet(int size) {
        array = new Object[size];
    }

    public ArIndexedSet(ArIndexedSet<K> set) {
        if (needSwitchToStored(set)) {
            switchToStored(set.size, set.array);
        } else if (!set.isStored()) {
            size = set.size;
            array = set.array.clone();
        } else {
            StoredArIndexedSet<K> storedSet = new StoredArIndexedSet<>(set.stored().getStoredArray());
            switchToStored(storedSet);
        }
    }

    public ArIndexedSet(StoredArray<K> keys) {
        switchToStored(new StoredArIndexedSet<>(keys));
    }
    
    public int size() {
        if (!isStored()) {
            return size;
        } else {
            return stored().size();
        }
    }
    
    public Object[] getArray() {
        if (!isStored()) {
            return array;
        } else {
            throw new UnsupportedOperationException();
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
        return new ArIndexedMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new ArIndexedMap<>(this);
    }

    @Override
    public boolean contains(K element) {
        if (!isStored()) {
            return ArIndexedMap.findIndex(element, size, array) >= 0;
        } else {
            return stored().contains(element);
        }
    }

    @Override
    public K getIdentIncl(K element) {
        if (!isStored()) {
            return get(ArIndexedMap.findIndex(element, size, array));
        } else {
            return stored().getIdentIncl(element);
        }
    }

    @Override
    public void keep(K element) {
        if (!isStored()) {
            assert size == 0 || array[size - 1].hashCode() <= element.hashCode();
            array[size++] = element;
            switchToStoredIfNeeded(size-1, size);
        } else {
            stored().keep(element);
        }
    }

    public boolean add(K element) {
        throw new UnsupportedOperationException();
    }

    public ImSet<K> immutable() {
        if (size() == 0)
            return SetFact.EMPTY();
        if (size() == 1)
            return SetFact.singleton(single());

        if (needSwitchToStored(this)) {
            switchToStored(size, array);
            return stored().immutable();
        }

        shrink();
        if (!isStored() && size < SetFact.useArrayMax) {
            return new ArSet<>(size, array);
        }
        return this;
    }

    public ImSet<K> immutableCopy() {
        return new ArIndexedSet<>(this);
    }

    public ArIndexedMap<K, K> toMap() {
        if (!isStored()) {
            return new ArIndexedMap<>(size, array, array);
        } else {
            return new ArIndexedMap<>(stored().getStoredArray(), stored().getStoredArray());
        }
    }

    public ImRevMap<K, K> toRevMap() {
        return toMap();
    }

    public ImOrderSet<K> toOrderSet() {
        return new ArOrderIndexedSet<>(this, ArSet.genOrder(size()));
    }

    public void shrink() {
        if (!isStored() && array.length > size * SetFact.factorNotResize) {
            Object[] newArray = new Object[size];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }
    }
    
    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        ArIndexedSet<?> set = (ArIndexedSet<?>) o;
        serializer.serialize(set.size, outStream);
        ArCol.serializeArray(set.array, serializer, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        int size = (int)serializer.deserialize(inStream);
        Object[] array = ArCol.deserializeArray(inStream, serializer);
        return new ArIndexedSet<>(size, array);
    }
    
    public boolean isStored() {
        return size == STORED_FLAG;
    }
    
    public StoredArIndexedSet<K> stored() {
        return (StoredArIndexedSet<K>) array[0];
    }

    private void switchToStoredIfNeeded(int oldSize, int newSize) {
        if (oldSize <= LIMIT && newSize > LIMIT && needSwitchToStored(this)) {
            switchToStored(size, array);
        }
    }
    
    private static boolean needSwitchToStored(ArIndexedSet<?> set) {
        return !set.isStored() && set.size() > LIMIT 
                && StoredArraySerializer.getInstance().canBeSerialized(set.get(0)); 
    }
    
    private void switchToStored(int size, Object[] array) {
        try {
            StoredArIndexedSet<K> storedSet = new StoredArIndexedSet<>(size, (K[]) array, StoredArraySerializer.getInstance());
            switchToStored(storedSet);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void switchToStored(StoredArIndexedSet<K> storedSet) {
        this.array = new Object[]{storedSet};
        this.size = STORED_FLAG;
    }
}

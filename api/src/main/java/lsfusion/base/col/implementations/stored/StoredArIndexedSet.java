package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.io.IOException;

public class StoredArIndexedSet<K> extends AMSet<K> {

    public StoredArray<K> array;
    
    public StoredArIndexedSet(StoredArray<K> array) {
        this.array = array;
    }

    public StoredArIndexedSet(StoredArraySerializer serializer, K[] array) throws IOException {
        this.array = new StoredArray<>(array, serializer);
    }
    
    public StoredArIndexedSet(StoredArraySerializer serializer, int size) throws IOException {
        array = new StoredArray<>(size, serializer);
    }

    public StoredArIndexedSet(StoredArIndexedSet<K> set) {
        // todo [dale]: this can be optimized
        array = set.array.clone();
    }

    public int size() {
        return array.size();
    }

    public K get(int i) {
        try {
            return array.get(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        try {
            return new StoredArIndexedMap<>(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        try {
            return new StoredArIndexedMap<>(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean contains(K element) {
        return StoredArIndexedMap.findIndex(element, array) >= 0;
    }

    @Override
    public K getIdentIncl(K element) {
        return get(StoredArIndexedMap.findIndex(element, array));
    }

    @Override
    public void keep(K element) {
        try {
            assert size() == 0 || array.get(size() - 1).hashCode() <= element.hashCode();
            array.append(element);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean add(K element) {
        throw new UnsupportedOperationException();
    }

    public ImSet<K> immutable() {
        return this;
    }

    public ImSet<K> immutableCopy() {
        return new StoredArIndexedSet<>(this);
    }

    public StoredArIndexedMap<K, K> toMap() {
        return new StoredArIndexedMap<K, K>(array, array);
    }

    public ImRevMap<K, K> toRevMap() {
        return toMap();
    }

    public ImOrderSet<K> toOrderSet() {
        // todo [dale]:
        throw new UnsupportedOperationException();
//        return new StoredArOrderIndexedSet<>(this, ArSet.genOrder(size));
    }
}

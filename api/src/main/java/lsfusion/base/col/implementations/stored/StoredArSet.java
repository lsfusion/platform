package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class StoredArSet<K> extends AMSet<K> {
    StoredArray<K> array;

    public StoredArSet(StoredArraySerializer serializer, int size, K[] array) {
        this.array = new StoredArray<>(array, size, serializer, null);
    }

    public StoredArSet(StoredArraySerializer serializer, int size) {
        this.array = new StoredArray<>(size, serializer);
    }

    public StoredArSet(StoredArraySerializer serializer, K[] array) {
        this(serializer, array.length, array);
    }
    
    public StoredArSet(StoredArSet<K> set) {
        this.array = new StoredArray<>(set.array);
    }

    public StoredArSet(StoredArray<K> array) {
        this.array = array;
    }
    
    @Override
    public int size() {
        return array.size();
    }

    @Override
    public K get(int i) {
        return array.get(i);
    }

    @Override
    public <M> ImValueMap<K, M> mapItValues() {
        return new StoredArMap<>(this);
    }

    @Override
    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new StoredArMap<>(this);
    }

    @Override
    public void exclAdd(K key) { // не проверяем, чтобы в профайлере не мусорить
//        assert !contains(key);
        array.append(key);
    }
    
    // very slow O(n) operation 
    @Override
    public boolean add(K element) {
        throw new UnsupportedOperationException();
//        for (int i = 0; i < array.size(); ++i)
//            if (BaseUtils.hashEquals(array.get(i), element))
//                return true;
//        exclAdd(element);
//        return false;  
    }

    public ImSet<K> immutableCopy() {
        return new StoredArSet<>(this);
    }

    @Override
    public ImSet<K> immutable() {
//        if (size() == 0)
//            return SetFact.EMPTY();
//        if (size() == 1)
//            return SetFact.singleton(single());
//
//        if (size() < SetFact.useArrayMax)
//            return this;
//
        throw new UnsupportedOperationException();
    }

    @Override
    public StoredArMap<K, K> toMap() {
        return new StoredArMap<>(array, array);
    }

    @Override
    public ImRevMap<K, K> toRevMap() {
        return toMap();
    }

    @Override
    public ImOrderSet<K> toOrderSet() {
        return new StoredArOrderSet<>(this);
    }
    
}

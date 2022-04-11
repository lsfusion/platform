package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMWrapOrderMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

import java.util.stream.IntStream;

public class StoredArOrderMap<K, V> extends AMWrapOrderMap<K, V, StoredArMap<K, V>> {
    public StoredArOrderMap(StoredArMap<K, V> wrapMap) {
        super(wrapMap);
    }

    public StoredArOrderMap(StoredArOrderMap<K, ?> orderMap) {
        super(new StoredArMap<>(orderMap.wrapMap));
    }
    
    public StoredArOrderMap(StoredArOrderMap<K, V> orderMap, boolean clone) {
        super(new StoredArMap<>(orderMap.wrapMap, clone));
        assert clone;
    }

    public MOrderExclMap<K, V> orderCopy() {
        return new StoredArOrderMap<>(this, true);
    }

    public StoredArOrderMap(StoredArOrderSet<K> orderSet) {
        super(new StoredArMap<>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new StoredArOrderMap<>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        int[] order = new int[wrapMap.size()];
        StoredArIndexedMap<K, V> indexedMap = wrapMap.toStoredArIndexedMap(order);
        Integer[] objectOrder = IntStream.of(order).boxed().toArray(Integer[]::new);
        return new StoredArOrderIndexedMap<>(indexedMap, new StoredArray<>(objectOrder, StoredArraySerializer.getInstance()));
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new StoredArOrderSet<>(new StoredArSet<K>(wrapMap.keys()));
    }

    @Override
    public ImList<V> valuesList() {
        throw new UnsupportedOperationException(); // ??
//        return new ArList<>(new ArCol<>(wrapMap.size, wrapMap.values));
    }
}

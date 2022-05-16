package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMWrapOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class StoredHOrderMap<K, V> extends AMWrapOrderMap<K, V, StoredHMap<K, V>> {

    public StoredHOrderMap(StoredHMap<K, V> wrapMap) {
        super(wrapMap);
    }

    public StoredHOrderMap(StoredHOrderMap<K, V> orderMap, AddValue<K, V> addValue) {
        super(new StoredHMap<>(orderMap.wrapMap, addValue));
    }

    // ImValueMap
    public StoredHOrderMap(StoredHOrderMap<K, ?> orderMap) {
        super(new StoredHMap<>(orderMap.wrapMap));
    }

    public StoredHOrderMap(StoredHOrderSet<K> hOrderSet) {
        super(new StoredHMap<>(hOrderSet.wrapSet));
    }

    public StoredHOrderMap(StoredHOrderMap<K, V> orderMap, boolean clone) {
        super(new StoredHMap<>(orderMap.wrapMap, clone));
        assert clone;
    }

    public MOrderExclMap<K, V> orderCopy() {
        return new StoredHOrderMap<>(this, true);
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new StoredHOrderMap<>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        return this;
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new StoredHOrderSet<>(new StoredHSet<>(wrapMap.size(), wrapMap.getStoredKeys(), wrapMap.getIndexes()));
    }
}

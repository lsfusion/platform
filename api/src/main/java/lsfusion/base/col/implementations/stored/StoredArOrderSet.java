package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMWrapOrderSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

import java.util.stream.IntStream;

public class StoredArOrderSet<K> extends AMWrapOrderSet<K, StoredArSet<K>> {
    public StoredArOrderSet(StoredArSet<K> wrapSet) {
        super(wrapSet);
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new StoredArOrderMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() { // предполагается заполнение в том же порядке
        return new StoredArMap<>(wrapSet);
    }

    public ImOrderSet<K> immutableOrder() {
        // todo [dale]: improve this method
        int[] order = new int[wrapSet.size()];
        StoredArIndexedSet<K> indexedSet = wrapSet.toStoredArIndexedSet(order);
        Integer[] objectOrder = IntStream.of(order).boxed().toArray(Integer[]::new);
        return new StoredArOrderIndexedSet<>(indexedSet, new StoredArray<>(objectOrder, StoredArraySerializer.getInstance()));
    }
}

package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMWrapOrderSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class StoredHOrderSet<K> extends AMWrapOrderSet<K, StoredHSet<K>> {
    public StoredHOrderSet(StoredHSet<K> wrapSet) {
        super(wrapSet);
    }

    public StoredHOrderSet(StoredHOrderSet<K> orderSet) {
        super(new StoredHSet<>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new StoredHOrderMap<>(this);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() { // предполагается заполнение в том же порядке
        return new StoredHMap<>(wrapSet);
    }

    public ImOrderSet<K> immutableOrder() {
        return this;
    }
}

package lsfusion.base.col.implementations.stored;

import lsfusion.base.col.implementations.abs.AMWrapOrderSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

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
//        if(wrapSet.size() == 0)
//            return SetFact.EMPTYORDER();
//        if(wrapSet.size() == 1)
//            return SetFact.singletonOrder(single());
//
//        if(wrapSet.size() < SetFact.useArrayMax)
//            return this;
//
        throw new UnsupportedOperationException();
    }
}

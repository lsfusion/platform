package lsfusion.base.col.implementations.order;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.abs.AMWrapOrderSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class ArOrderSet<K> extends AMWrapOrderSet<K, ArSet<K>> {

    public ArOrderSet() {
        super(new ArSet<K>());
    }

    public ArOrderSet(int size) {
        super(new ArSet<K>(size));
    }

    public ArOrderSet(ArSet<K> wrapSet) {
        super(wrapSet);
    }

    public ArOrderSet(ArOrderSet<K> orderSet) {
        super(new ArSet<K>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderMap<K, M>(this);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() { // предполагается заполнение в том же порядке
        return new ArMap<K, M>(wrapSet);
    }

    public ImOrderSet<K> immutableOrder() {
        if(wrapSet.size==0)
            return SetFact.EMPTYORDER();
        if(wrapSet.size==1)
            return SetFact.singletonOrder(single());

        if(wrapSet.array.length > wrapSet.size * SetFact.factorNotResize) {
            Object[] newArray = new Object[wrapSet.size];
            System.arraycopy(wrapSet.array, 0, newArray, 0, wrapSet.size);
            wrapSet.array = newArray;
        }

        if(wrapSet.size < SetFact.useArrayMax)
            return this;

        // упорядочиваем Set
        int[] order = new int[wrapSet.size];
        ArSet.sortArray(wrapSet.size, wrapSet.array, order);
        return new ArOrderIndexedSet<K>(new ArIndexedSet<K>(wrapSet.size, wrapSet.array), order);
    }
}

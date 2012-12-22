package platform.base.col.implementations.order;

import platform.base.col.SetFact;
import platform.base.col.implementations.ArIndexedSet;
import platform.base.col.implementations.ArSet;
import platform.base.col.implementations.HMap;
import platform.base.col.implementations.HSet;
import platform.base.col.implementations.abs.AMWrapOrderSet;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class HOrderSet<K> extends AMWrapOrderSet<K, HSet<K>> {

    // mutable конструктор
    public HOrderSet() {
        super(new HSet<K>());
    }

    public HOrderSet(HSet<K> wrapSet) {
        super(wrapSet);
    }

    public HOrderSet(int size) {
        super(new HSet<K>(size));
    }

    public HOrderSet(HOrderSet<K> orderSet) {
        super(new HSet<K>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new HOrderMap<K, M>(this);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() { // предполагается заполнение в том же порядке
        return new HMap<K, M>(wrapSet);
    }

    public ImOrderSet<K> immutableOrder() {
        if(wrapSet.size==0)
            return SetFact.EMPTYORDER();
        if(wrapSet.size==1)
            return SetFact.singletonOrder(single());

        if(wrapSet.size < SetFact.useArrayMax) {
            Object[] array = new Object[wrapSet.size];
            for(int i=0;i<wrapSet.size;i++)
                array[i] = get(i);
            return new ArOrderSet<K>(new ArSet<K>(wrapSet.size, array));
        }
        if(wrapSet.size >= SetFact.useIndexedArrayMin) {
            Object[] array = new Object[wrapSet.size];
            for(int i=0;i<wrapSet.size;i++)
                array[i] = get(i);
            int[] order = new int[wrapSet.size];
            ArSet.sortArray(wrapSet.size, array, order);
            return new ArOrderIndexedSet<K>(new ArIndexedSet<K>(wrapSet.size, array), order);
        }

        if(wrapSet.indexes.length > wrapSet.size * SetFact.factorNotResize) {
            int[] newIndexes = new int[wrapSet.size];
            System.arraycopy(wrapSet.indexes, 0, newIndexes, 0, wrapSet.size);
            wrapSet.indexes = newIndexes;
        }
        return this;
    }

}

package lsfusion.base.col.implementations.order;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.abs.AMWrapOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class HOrderMap<K, V> extends AMWrapOrderMap<K, V, HMap<K, V>> {
    
    public HOrderMap(AddValue<K, V> addValue) {
        super(new HMap<K, V>(addValue));
    }

    public HOrderMap(HMap<K, V> wrapMap) {
        super(wrapMap);
    }

    public HOrderMap(HOrderMap<K, V> orderMap, AddValue<K, V> addValue) {
        super(new HMap<K, V>(orderMap.wrapMap, addValue));
    }

    public HOrderMap(int size, AddValue<K, V> addValue) {
        super(new HMap<K, V>(size, addValue));
    }

    // ImValueMap
    public HOrderMap(HOrderMap<K, ?> orderMap) {
        super(new HMap<K, V>(orderMap.wrapMap));
    }

    public HOrderMap(HOrderSet<K> hOrderSet) {
        super(new HMap<K, V>(hOrderSet.wrapSet));
    }

    public HOrderMap(HOrderMap<K, V> orderMap, boolean clone) {
        super(new HMap<K, V>(orderMap.wrapMap, clone));
        assert clone;
    }

    public MOrderExclMap<K, V> orderCopy() {
        return new HOrderMap<K, V>(this, true);
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new HOrderMap<K, M>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        if(wrapMap.size==0)
            return MapFact.EMPTYORDER();
        if(wrapMap.size==1)
            return MapFact.singletonOrder(singleKey(), singleValue());

        if(wrapMap.size < SetFact.useArrayMax) {
            Object[] keys = new Object[wrapMap.size];
            Object[] values = new Object[wrapMap.size];
            for(int i=0;i<wrapMap.size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            return new ArOrderMap<K, V>(new ArMap<K, V>(wrapMap.size, keys, values));
        }
        if(wrapMap.size >= SetFact.useIndexedArrayMin) {
            Object[] keys = new Object[wrapMap.size];
            Object[] values = new Object[wrapMap.size];
            for(int i=0;i<wrapMap.size;i++) {
                keys[i] = getKey(i);
                values[i] = getValue(i);
            }
            int[] order = new int[wrapMap.size];
            ArSet.sortArray(wrapMap.size, keys, values, order);
            return new ArOrderIndexedMap<K, V>(new ArIndexedMap<K, V>(wrapMap.size, keys, values), order);
        }

        if(wrapMap.indexes.length > wrapMap.size * SetFact.factorNotResize) {
            int[] newIndexes = new int[wrapMap.size];
            System.arraycopy(wrapMap.indexes, 0, newIndexes, 0, wrapMap.size);
            wrapMap.indexes = newIndexes;
        }
        return this;
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new HOrderSet<K>(new HSet<K>(wrapMap.size, wrapMap.table, wrapMap.indexes));
    }
}

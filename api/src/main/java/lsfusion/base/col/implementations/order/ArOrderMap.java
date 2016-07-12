package lsfusion.base.col.implementations.order;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.*;
import lsfusion.base.col.implementations.abs.AMWrapOrderMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.MOrderExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;

public class ArOrderMap<K, V> extends AMWrapOrderMap<K, V, ArMap<K, V>> {

    public ArOrderMap(AddValue<K, V> addValue) {
        super(new ArMap<K, V>(addValue));
    }

    public ArOrderMap(ArMap<K, V> wrapMap) {
        super(wrapMap);
    }

    public ArOrderMap(ArOrderMap<K, V> orderMap, AddValue<K, V> addValue) {
        super(new ArMap<K, V>(orderMap.wrapMap, addValue));
    }

    public ArOrderMap(int size, AddValue<K, V> addValue) {
        super(new ArMap<K, V>(size, addValue));
    }

    // ImValueMap
    public ArOrderMap(ArOrderMap<K, ?> orderMap) {
        super(new ArMap<K, V>(orderMap.wrapMap));
    }

    public ArOrderMap(ArOrderMap<K, V> orderMap, boolean clone) {
        super(new ArMap<K, V>(orderMap.wrapMap, clone));
        assert clone;
    }

    public MOrderExclMap<K, V> orderCopy() {
        return new ArOrderMap<K, V>(this, true);
    }

    public ArOrderMap(ArOrderSet<K> orderSet) {
        super(new ArMap<K, V>(orderSet.wrapSet));
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderMap<K, M>(this);
    }

    public ImOrderMap<K, V> immutableOrder() {
        if(wrapMap.size==0)
            return MapFact.EMPTYORDER();
        if(wrapMap.size==1)
            return MapFact.singletonOrder(singleKey(), singleValue());

        if(wrapMap.keys.length > wrapMap.size * SetFact.factorNotResize) {
            Object[] newKeys = new Object[wrapMap.size];
            System.arraycopy(wrapMap.keys, 0, newKeys, 0, wrapMap.size);
            wrapMap.keys = newKeys;
            Object[] newValues = new Object[wrapMap.size];
            System.arraycopy(wrapMap.values, 0, newValues, 0, wrapMap.size);
            wrapMap.values = newValues;
        }

        if(wrapMap.size < SetFact.useArrayMax)
            return this;

        // упорядочиваем Set
        int[] order = new int[wrapMap.size];
        ArSet.sortArray(wrapMap.size, wrapMap.keys, wrapMap.values, order);
        return new ArOrderIndexedMap<K, V>(new ArIndexedMap<K, V>(wrapMap.size, wrapMap.keys, wrapMap.values), order);
    }

    @Override
    public ImOrderSet<K> keyOrderSet() {
        return new ArOrderSet<K>(new ArSet<K>(wrapMap.size, wrapMap.keys));
    }

    @Override
    public ImList<V> valuesList() {
        return new ArList<V>(new ArCol<V>(wrapMap.size, wrapMap.values));
    }
}

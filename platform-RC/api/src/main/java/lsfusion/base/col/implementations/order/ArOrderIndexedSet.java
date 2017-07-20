package lsfusion.base.col.implementations.order;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.ArIndexedMap;
import lsfusion.base.col.implementations.ArIndexedSet;
import lsfusion.base.col.implementations.ArSet;
import lsfusion.base.col.implementations.abs.AMOrderSet;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class ArOrderIndexedSet<K> extends AMOrderSet<K> {
    
    public ArIndexedSet<K> arSet; // для дружественных классов
    public int[] order;

    public ArOrderIndexedSet(int size) {
        arSet = new ArIndexedSet<>(size);
        order = new int[size];
    }

    public ArOrderIndexedSet(ArIndexedSet<K> arSet, int[] order) {
        this.arSet = arSet;
        this.order = order;
    }

    public ImSet<K> getSet() {
        return arSet;
    }

    public int size() {
        return arSet.size();
    }

    public K get(int i) {
        return arSet.get(order[i]);
    }

    public boolean add(K key) {
        throw new UnsupportedOperationException();
    }

    public void exclAdd(K key) {
        throw new UnsupportedOperationException();
    }
    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderIndexedMap<>(this);
    }

    private class RevMap<V> implements ImRevValueMap<K, V> {
        private ArIndexedMap<K, V> result = new ArIndexedMap<>(arSet);

        public void mapValue(int i, V value) {
            result.mapValue(order[i], value);
        }

        public ImRevMap<K, V> immutableValueRev() {
            return result.immutableValueRev();
        }
    }
    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new RevMap<>();
    }

    public ImOrderSet<K> immutableOrder() {
        if(arSet.size==0)
            return SetFact.EMPTYORDER();
        if(arSet.size==1)
            return SetFact.singletonOrder(single());

        if(arSet.size < SetFact.useArrayMax) {
            Object[] orderArray = new Object[arSet.size];
            for(int i=0;i<arSet.size;i++)
                orderArray[i] = get(i);
            return new ArOrderSet<>(new ArSet<K>(arSet.size, orderArray));
        }

        if(arSet.array.length > arSet.size * SetFact.factorNotResize) {
            Object[] newArray = new Object[arSet.size];
            System.arraycopy(arSet.array, 0, newArray, 0, arSet.size);
            arSet.array = newArray;
        }

        return this;
    }
}

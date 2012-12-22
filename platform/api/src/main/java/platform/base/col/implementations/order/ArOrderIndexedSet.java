package platform.base.col.implementations.order;

import platform.base.col.SetFact;
import platform.base.col.implementations.ArIndexedMap;
import platform.base.col.implementations.ArIndexedSet;
import platform.base.col.implementations.ArSet;
import platform.base.col.implementations.abs.AMOrderSet;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;

public class ArOrderIndexedSet<K> extends AMOrderSet<K> {
    
    public ArIndexedSet<K> arSet; // для дружественных классов
    public int[] order;

    public ArOrderIndexedSet(int size) {
        arSet = new ArIndexedSet<K>(size);
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

    public void add(K key) {
        throw new UnsupportedOperationException();
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new ArOrderIndexedMap<K, M>(this);
    }

    private class RevMap<V> implements ImRevValueMap<K, V> {
        private ArIndexedMap<K, V> result = new ArIndexedMap<K, V>(arSet);

        public void mapValue(int i, V value) {
            result.mapValue(order[i], value);
        }

        public ImRevMap<K, V> immutableValueRev() {
            return result.immutableValueRev();
        }
    }
    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new RevMap<M>();
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
            return new ArOrderSet<K>(new ArSet<K>(arSet.size, orderArray));
        }

        if(arSet.array.length > arSet.size * SetFact.factorNotResize) {
            Object[] newArray = new Object[arSet.size];
            System.arraycopy(arSet.array, 0, newArray, 0, arSet.size);
            arSet.array = newArray;
        }

        return this;
    }
}

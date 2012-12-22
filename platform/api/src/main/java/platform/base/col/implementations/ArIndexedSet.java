package platform.base.col.implementations;

import platform.base.BaseUtils;
import platform.base.col.SetFact;
import platform.base.col.implementations.abs.AMSet;
import platform.base.col.implementations.order.ArOrderIndexedSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class ArIndexedSet<K> extends AMSet<K> {

    public int size;
    public Object[] array;

    public ArIndexedSet() {
        this.array = new Object[4];
    }

    public ArIndexedSet(int size, Object[] array) {
        this.size = size;
        this.array = array;
    }

    public ArIndexedSet(int size) {
        array = new Object[size];
    }

    public ArIndexedSet(ArIndexedSet<K> set) {
        size = set.size;
        array = set.array.clone();
    }

    public int size() {
        return size;
    }

    public K get(int i) {
        return (K) array[i];
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new ArIndexedMap<K, M>(this);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new ArIndexedMap<K, M>(this);
    }

    @Override
    public boolean contains(K element) {
        return ArIndexedMap.findIndex(element, size, array) >= 0;
    }

    @Override
    public void keep(K element) {
        assert size==0 || array[size-1].hashCode() <= element.hashCode();
        array[size++] = element;
    }

    public boolean add(K element) {
        throw new UnsupportedOperationException();
    }

    public ImSet<K> immutable() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if(array.length > size * SetFact.factorNotResize) {
            Object[] newArray = new Object[size];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }

        if(size < SetFact.useArrayMax)
            return new ArSet<K>(size, array);

        return this;
    }

    public ImSet<K> immutableCopy() {
        return new ArIndexedSet<K>(this);
    }

    public ArIndexedMap<K, K> toMap() {
        return new ArIndexedMap<K, K>(size, array, array);
    }

    public ImRevMap<K, K> toRevMap() {
        return toMap();
    }

    public ImOrderSet<K> toOrderSet() {
        return new ArOrderIndexedSet<K>(this, ArSet.genOrder(size));
    }
}

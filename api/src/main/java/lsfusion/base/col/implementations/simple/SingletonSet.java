package lsfusion.base.col.implementations.simple;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.*;

public class SingletonSet<K> implements ImSet<K>, ImList<K>, ImOrderSet<K> {
    
    private final K key;

    public SingletonSet(K key) {
//        assert !(key instanceof ImmutableK);
        this.key = key;
    }

    public int size() {
        return 1;
    }

    public K get(int i) {
        assert i==0;
        return key;
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new SingletonRevMap<K, M>(key);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new SingletonRevMap<K, M>(key);
    }

    public boolean equals(Object obj) {
        if(this==obj)
            return true;

        if(obj instanceof ImCol)
            return ((ImCol)obj).size()==1 && key.equals(((ImCol)obj).single());
        if(obj instanceof ImList)
            return ((ImList)obj).size()==1 && key.equals(((ImList)obj).single());
        return false;
    }

    public int hashCode() {
        return key.hashCode();
    }

    private class SingleIterator implements Iterator<K> {

        private boolean hasNext = true;

        public boolean hasNext() {
            return hasNext;
        }

        public K next() {
            hasNext = false;
            return SingletonSet.this.key;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    public Iterator<K> iterator() {
        return new SingleIterator();
    }

    public boolean contains(K element) {
        return BaseUtils.hashEquals(key, element);
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isFull() {
        return false;
    }

    public K single() {
        return key;
    }

    public ImSet<K> toSet() {
        return this;
    }

    public ImList<K> toList() {
        return this;
    }

    public ImCol<K> mergeCol(ImCol<K> col) {
        MCol<K> mCol = ListFact.mCol(col);
        mCol.add(key);
        return mCol.immutableCol();
    }

    public ImCol<K> filterCol(FunctionSet<K> filter) {
        if(filter.contains(key))
            return this;
        return SetFact.EMPTY();
    }

    public ImMap<K, Integer> multiSet() {
        return MapFact.<K, Integer>singletonRev(key, 1);
    }

    public <M> ImCol<M> mapColValues(GetIndexValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(0, key));
    }

    public <M> ImCol<M> mapColValues(GetValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(key));
    }

    public <M> ImSet<M> mapColSetValues(GetIndexValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(0, key));
    }

    public <M> ImSet<M> mapColSetValues(GetValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(key));
    }

    public <M> ImSet<M> mapMergeSetValues(GetValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(key));
    }

    public <M> ImMap<M, K> mapColKeys(GetIndex<M> getter) {
        return MapFact.<M, K>singleton(getter.getMapValue(0), key);
    }

    public String toString(String separator) {
        return key.toString();
    }

    public String toString(GetValue<String, K> getter, String delimiter) {
        return getter.getMapValue(key);
    }

    public String toString(GetStaticValue<String> getter, String delimiter) {
        return getter.getMapValue();
    }

    public ImList<K> sort(Comparator<K> comparator) {
        return this;
    }

    public Collection<K> toJavaCol() {
        return Collections.<K>singleton(key);
    }

    public ImSet<K> getSet() {
        return this;
    }

    public ImOrderSet<K> addOrderExcl(ImOrderSet<? extends K> map) {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet();
        mResult.exclAdd(key);
        mResult.exclAddAll(map);
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> addOrderExcl(K element) {
        MOrderExclSet<K> mResult = SetFact.mOrderExclSet(2);
        mResult.exclAdd(key);
        mResult.exclAdd(element);
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> mergeOrder(ImOrderSet<? extends K> col) {
        MOrderSet<K> mResult = SetFact.mOrderSet();
        mResult.add(key);
        mResult.addAll(col);
        return mResult.immutableOrder();
    }

    public ImOrderSet<K> mergeOrder(K element) {
        if(BaseUtils.hashEquals(key, element))
            return this;

        return addOrderExcl(element);
    }

    public <V> ImRevMap<K, V> mapSet(ImOrderSet<? extends V> set) {
        return MapFact.<K, V>singletonRev(key, set.get(0));
    }

    public <V> ImMap<K, V> mapList(ImList<? extends V> list) {
        return MapFact.<K, V>singleton(key, list.get(0));
    }

    public ImOrderSet<K> removeOrder(ImSet<? extends K> set) {
        if(((ImSet<K>)set).contains(key))
            return SetFact.EMPTYORDER();
        return this;
    }

    public ImOrderSet<K> removeOrderIncl(K element) {
        assert BaseUtils.hashEquals(key, element);
        return SetFact.EMPTYORDER();
    }

    public <V> ImOrderSet<V> mapOrder(ImRevMap<? extends K, ? extends V> map) {
        return SetFact.singletonOrder(((ImRevMap<K, V>) map).get(key));
    }

    public <V> ImOrderSet<V> mapOrder(ImMap<? extends K, ? extends V> map) {
        return SetFact.singletonOrder(((ImRevMap<K, V>)map).get(key));
    }

    public <V> ImOrderMap<K, V> mapOrderMap(ImMap<K, V> map) {
        return MapFact.<K, V>singletonOrder(key, map.get(key));
    }

    public ImOrderSet<K> reverseOrder() {
        return this;
    }

    public ImOrderSet<K> filterOrder(FunctionSet<K> filter) {
        if(filter.contains(key))
            return this;
        return SetFact.EMPTYORDER();
    }

    public ImOrderSet<K> filterOrderIncl(ImSet<? extends K> set) {
        if(set.size()==0)
            return SetFact.EMPTYORDER();
        assert set.size()==1 && BaseUtils.hashEquals(key, set.single());
        return this;
    }

    public ImOrderSet<K> subOrder(int from, int to) {
        if(from==0 && to == 1)
            return this;
        return SetFact.EMPTYORDER();
    }

    public <M> ImOrderValueMap<K, M> mapItOrderValues() {
        return new SingletonOrderMap<K, M>(key);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new SingletonRevMap<K, M>(key);
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetValue<M, K> getter) {
        return SetFact.singletonOrder(getter.getMapValue(key));
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetIndexValue<M, K> getter) {
        return SetFact.singletonOrder(getter.getMapValue(0, key));
    }

    public <M> ImOrderSet<M> mapMergeOrderSetValues(GetValue<M, K> getter) {
        return SetFact.singletonOrder(getter.getMapValue(key));
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetStaticValue<M> getter) {
        return MapFact.<K, M>singletonOrder(key, getter.getMapValue());
    }

    public <M> ImOrderMap<K, M> mapOrderValues(GetValue<M, K> getter) {
        return MapFact.<K, M>singletonOrder(key, getter.getMapValue(key));
    }

    public <M> ImMap<K, M> mapOrderValues(GetIndexValue<M, K> getter) {
        return MapFact.<K, M>singleton(key, getter.getMapValue(0, key));
    }

    public <M> ImMap<K, M> mapOrderValues(GetIndex<M> getter) {
        return MapFact.<K, M>singleton(key, getter.getMapValue(0));
    }

    public <M> ImRevMap<K, M> mapOrderRevValues(GetIndex<M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.getMapValue(0));
    }

    public <M> ImRevMap<K, M> mapOrderRevValues(GetIndexValue<M, K> getter) {
        return MapFact.<K, M>singletonRev(key, getter.getMapValue(0, key));
    }

    public <M> ImRevMap<M, K> mapOrderRevKeys(GetIndex<M> getter) {
        return MapFact.<M, K>singletonRev(getter.getMapValue(0), key);
    }

    public <M> ImRevMap<M, K> mapOrderRevKeys(GetIndexValue<M, K> getter) {
        return MapFact.<M, K>singletonRev(getter.getMapValue(0, key), key);
    }

    public <V> ImOrderMap<K, V> toOrderMap(V value) {
        return MapFact.<K, V>singletonOrder(key, value);
    }

    public K[] toArray(K[] array) {
        array[0] = key;
        return array;
    }

    public ImCol<K> getCol() {
        return this;
    }

    public int indexOf(K key) {
        if(BaseUtils.hashEquals(key, key))
            return 0;
        return -1;
    }

    public ImMap<Integer, K> toIndexedMap() {
        return MapFact.<Integer, K>singleton(0, key);
    }

    public ImList<K> addList(ImList<? extends K> list) {
        MList<K> mResult = ListFact.mList(list.size()+1);
        mResult.add(key);
        mResult.addAll(list);
        return mResult.immutableList();
    }

    public ImList<K> addList(K element) {
        return ListFact.toList(key, element);
    }

    public ImList<K> reverseList() {
        return this;
    }

    public ImList<K> subList(int i, int to) {
        if(i == 0 && to == 1)
            return this;
        return ListFact.EMPTY();
    }

    public <V> ImList<V> mapList(ImMap<? extends K, ? extends V> imMap) {
        return ListFact.singleton(((ImMap<K, V>)imMap).get(key));
    }

    public ImOrderSet<K> toOrderExclSet() {
        return this;
    }

    public ImList<K> filterList(FunctionSet<K> filter) {
        if(filter.contains(key))
            return this;
        return ListFact.EMPTY();
    }

    public <M> ImList<M> mapItListValues(GetValue<M, K> getter) {
        return ListFact.singleton(getter.getMapValue(key));
    }

    public <M> ImList<M> mapListValues(GetIndex<M> getter) {
        return ListFact.singleton(getter.getMapValue(0));
    }

    public <M> ImList<M> mapListValues(GetIndexValue<M, K> getter) {
        return ListFact.singleton(getter.getMapValue(0, key));
    }

    public <M> ImList<M> mapListValues(GetValue<M, K> getter) {
        return ListFact.singleton(getter.getMapValue(key));
    }

    public <M> ImMap<M, K> mapListMapValues(GetIndex<M> getterKey) {
        return MapFact.<M, K>singleton(getterKey.getMapValue(0), key);
    }

    public <MK, MV> ImMap<MK, MV> mapListKeyValues(GetIndex<MK> getterKey, GetValue<MV, K> getterValue) {
        return MapFact.singleton(getterKey.getMapValue(0), getterValue.getMapValue(key));
    }

    public <MK, MV> ImRevMap<MK, MV> mapListRevKeyValues(GetIndex<MK> getterKey, GetValue<MV, K> getterValue) {
        return MapFact.singletonRev(getterKey.getMapValue(0), getterValue.getMapValue(key));
    }

    public String toString(GetIndexValue<String, K> getter, String delimiter) {
        return getter.getMapValue(0, key);
    }

    public List<K> toJavaList() {
        return Collections.<K>singletonList(key);
    }

    public boolean intersect(ImSet<? extends K> set) {
        return ((ImSet<K>)set).contains(key);
    }

    public boolean intersect(FunctionSet<? extends K> set) {
        return ((FunctionSet<K>)set).contains(key);
    }

    public boolean disjoint(ImSet<? extends K> col) {
        return !((ImSet<K>)col).contains(key);
    }

    public boolean containsAll(ImSet<? extends K> wheres) {
        if(wheres.isEmpty())
            return true;

        return wheres.size() == 1 && BaseUtils.hashEquals(key, wheres.single());
    }

    public <G> ImMap<G, ImSet<K>> group(BaseUtils.Group<G, K> getter) {
        return MapFact.<G, ImSet<K>>singleton(getter.group(key), this);
    }

    public <V> ImCol<V> map(ImMap<K, ? extends V> map) {
        return SetFact.singleton(map.get(key));
    }

    public <EV> ImSet<EV> mapRev(ImRevMap<K, EV> map) {
        return SetFact.singleton(map.get(key));
    }

    public ImSet<K> merge(ImSet<? extends K> merge) {
        if(merge.isEmpty())
            return this;
        if(((ImSet<K>)merge).contains(key))
            return (ImSet<K>) merge;

        MSet<K> mSet = SetFact.mSet(merge);
        mSet.add(key);
        return mSet.immutable();
    }

    public ImSet<K> merge(K element) {
        if(BaseUtils.hashEquals(key, element))
            return this;

        MExclSet<K> mSet = SetFact.mExclSet(2);
        mSet.exclAdd(element);
        mSet.exclAdd(key);
        return mSet.immutable();
    }

    @Override
    public ImSet<K> addExcl(ImSet<? extends K> merge) {
        if(merge.isEmpty())
            return this;

        MExclSet<K> mSet = SetFact.mExclSet(merge);
        mSet.exclAddAll(this);
        return mSet.immutable();
    }

    public ImSet<K> addExcl(K element) {
        MExclSet<K> mSet = SetFact.mExclSet(2);
        mSet.exclAdd(element);
        mSet.exclAdd(key);
        return mSet.immutable();
    }

    public <M> ImFilterValueMap<K, M> mapFilterValues() {
        return new FilterValueMap<K, M>(this.<M>mapItValues());
    }

    public ImSet<K> filterFn(FunctionSet<K> filter) {
        if(filter.contains(key))
            return this;
        return SetFact.EMPTY();
    }

    public ImSet<K> split(FunctionSet<K> filter, Result<ImSet<K>> rest) {
        if(filter.contains(key)) {
            rest.set(SetFact.<K>EMPTY());
            return this;
        }
        rest.set(this);
        return SetFact.EMPTY();
    }

    public ImSet<K> filter(ImSet<? extends K> filter) {
        if(((ImSet<K>)filter).contains(key))
            return this;
        return SetFact.EMPTY();
    }

    public ImSet<K> remove(ImSet<? extends K> remove) {
        if(((ImSet<K>)remove).contains(key))
            return SetFact.EMPTY();
        return this;
    }

    public ImSet<K> removeIncl(ImSet<? extends K> remove) {
        if(remove.isEmpty())
            return this;

        assert BaseUtils.hashEquals(key, remove.single());
        return SetFact.EMPTY();
    }

    public ImSet<K> removeIncl(K element) {
        assert BaseUtils.hashEquals(key, element);
        return SetFact.EMPTY();
    }

    public <V> ImMap<K, V> toMap(V value) {
        return MapFact.<K, V>singleton(key, value);
    }

    public ImMap<K, K> toMap() {
        return MapFact.<K, K>singleton(key, key);
    }

    public ImRevMap<K, K> toRevMap() {
        return MapFact.<K, K>singletonRev(key, key);
    }

    public ImOrderSet<K> toOrderSet() {
        return this;
    }

    public ImOrderSet<K> sort() {
        return this;
    }

    public <M> ImMap<K, M> mapItValues(GetValue<M, K> getter) {
        return MapFact.<K, M>singleton(key, getter.getMapValue(key));
    }

    public <M> ImSet<M> mapItSetValues(GetValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(key));
    }

    public <M> ImSet<M> mapSetValues(GetValue<M, K> getter) {
        return SetFact.singleton(getter.getMapValue(key));
    }

    public <M> ImMap<K, M> mapValues(GetStaticValue<M> getter) {
        return MapFact.<K, M>singleton(key, getter.getMapValue());
    }

    public <M> ImMap<K, M> mapValues(GetIndex<M> getter) {
        return MapFact.<K, M>singleton(key, getter.getMapValue(0));
    }

    public <M> ImMap<K, M> mapValues(GetValue<M, K> getter) {
        return MapFact.<K, M>singleton(key, getter.getMapValue(key));
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(GetValue<MK, K> getterKey, GetValue<MV, K> getterValue) {
        return MapFact.singleton(getterKey.getMapValue(key), getterValue.getMapValue(key));
    }

    public <M> ImRevMap<K, M> mapRevValues(GetIndex<M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.getMapValue(0));
    }

    public <M> ImRevMap<K, M> mapRevValues(GetIndexValue<M, K> getter) {
        return MapFact.<K, M>singletonRev(key, getter.getMapValue(0, key));
    }

    public <M> ImRevMap<K, M> mapRevValues(GetStaticValue<M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.getMapValue());
    }

    public <M> ImRevMap<K, M> mapRevValues(GetValue<M, K> getter) {
        return MapFact.<K, M>singletonRev(key, getter.getMapValue(key));
    }

    public <M> ImRevMap<M, K> mapRevKeys(GetStaticValue<M> getter) {
        return MapFact.<M, K>singletonRev(getter.getMapValue(), key);
    }

    public <M> ImRevMap<M, K> mapRevKeys(GetValue<M, K> getter) {
        return MapFact.<M, K>singletonRev(getter.getMapValue(key), key);
    }

    public <M> ImRevMap<M, K> mapRevKeys(GetIndex<M> getter) {
        return MapFact.<M, K>singletonRev(getter.getMapValue(0), key);
    }

    public Set<K> toJavaSet() {
        return Collections.<K>singleton(key);
    }

    @Override
    public String toString() {
        return toString(",");
    }
}

package lsfusion.base;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.ACol;
import lsfusion.base.col.implementations.abs.ASet;
import lsfusion.base.col.implementations.simple.FilterValueMap;
import lsfusion.base.col.implementations.simple.SingletonOrderMap;
import lsfusion.base.col.implementations.simple.SingletonRevMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.*;

public abstract class ImmutableObject { // implements ImSet<Object>, ImList<Object>, ImOrderSet<Object> {
    
/*    private class SingleIterator implements Iterator<Object> {
        
        private boolean hasNext = true;

        public boolean hasNext() {
            return hasNext;
        }

        public Object next() {
            hasNext = false;
            return ImmutableObject.this;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    public Iterator<Object> iterator() {
        return new SingleIterator(); 
    }

    public <M> ImValueMap<Object, M> mapItValues() {
        return new SingletonRevMap<Object, M>(this);
    }

    public <M> ImRevValueMap<Object, M> mapItRevValues() {
        return new SingletonRevMap<Object, M>(this);
    }

    public boolean contains(Object element) {
        return BaseUtils.hashEquals(this, element);
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isFull() {
        return false;
    }

    public Object single() {
        return this;
    }

    public ImSet<Object> toSet() {
        return this;
    }

    public ImList<Object> toList() {
        return this;
    }

    public ImCol<Object> mergeCol(ImCol<Object> col) {
        MCol<Object> mCol = ListFact.mCol(col);
        mCol.add(this);
        return mCol.immutableCol();
    }

    public ImCol<Object> filterCol(FunctionSet<Object> filter) {
        if(filter.contains(this))
            return this;
        return SetFact.EMPTY();
    }

    public ImMap<Object, Integer> multiSet() {
        return MapFact.<Object, Integer>singletonRev(this, 1);
    }

    public <M> ImCol<M> mapColValues(GetIndexValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(0, this));
    }

    public <M> ImCol<M> mapColValues(GetValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(this));
    }

    public <M> ImSet<M> mapColSetValues(GetIndexValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(0, this));
    }

    public <M> ImSet<M> mapColSetValues(GetValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(this));
    }

    public <M> ImSet<M> mapMergeSetValues(GetValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(this));
    }

    public <M> ImMap<M, Object> mapColKeys(GetIndex<M> getter) {
        return MapFact.<M, Object>singleton(getter.getMapValue(0), this);
    }

    public String toString(String separator) {
        return toString();
    }

    public String toString(GetValue<String, Object> getter, String delimiter) {
        return getter.getMapValue(this);
    }

    public String toString(GetStaticValue<String> getter, String delimiter) {
        return getter.getMapValue();
    }

    public ImList<Object> sort(Comparator<Object> comparator) {
        return this;
    }

    public Collection<Object> toJavaCol() {
        return Collections.<Object>singleton(this);
    }

    public ImSet<Object> getSet() {
        return this;
    }

    public ImOrderSet<Object> addOrderExcl(ImOrderSet<? extends Object> map) {
        MOrderExclSet<Object> mResult = SetFact.mOrderExclSet(this);
        mResult.exclAddAll(map);
        return mResult.immutableOrder();
    }

    public ImOrderSet<Object> addOrderExcl(Object element) {
        MOrderExclSet<Object> mResult = SetFact.mOrderExclSet(2);
        mResult.exclAdd(this);
        mResult.exclAdd(element);
        return mResult.immutableOrder();
    }

    public ImOrderSet<Object> mergeOrder(ImOrderSet<? extends Object> col) {
        MOrderSet<Object> mResult = SetFact.mOrderSet(this);
        mResult.addAll(col);
        return mResult.immutableOrder();
    }

    public ImOrderSet<Object> mergeOrder(Object element) {
        if(BaseUtils.hashEquals(this, element))
            return this;
        
        return addOrderExcl(element);
    }

    public <V> ImRevMap<Object, V> mapSet(ImOrderSet<? extends V> set) {
        return MapFact.<Object, V>singletonRev(this, set.get(0)); 
    }

    public <V> ImMap<Object, V> mapList(ImList<? extends V> list) {
        return MapFact.<Object, V>singleton(this, list.get(0));
    }

    public ImOrderSet<Object> removeOrder(ImSet<? extends Object> set) {
        if(((ImSet<Object>)set).contains(this))
            return SetFact.EMPTYORDER();
        return this;
    }

    public ImOrderSet<Object> removeOrderIncl(Object element) {
        assert BaseUtils.hashEquals(this, element);
        return SetFact.EMPTYORDER();
    }

    public <V> ImOrderSet<V> mapOrder(ImRevMap<? extends Object, ? extends V> map) {
        return SetFact.singletonOrder(((ImRevMap<Object, V>) map).get(this));
    }

    public <V> ImOrderSet<V> mapOrder(ImMap<? extends Object, ? extends V> map) {
        return SetFact.singletonOrder(((ImRevMap<Object, V>)map).get(this));
    }

    public <V> ImOrderMap<Object, V> mapOrderMap(ImMap<Object, V> map) {
        return MapFact.<Object, V>singletonOrder(this, map.get(this));
    }

    public ImOrderSet<Object> reverseOrder() {
        return this;
    }

    public ImOrderSet<Object> filterOrder(FunctionSet<Object> filter) {
        if(filter.contains(this))
            return this;
        return SetFact.EMPTYORDER();
    }

    public ImOrderSet<Object> filterOrderIncl(ImSet<? extends Object> set) {
        if(set.size()==0)
            return SetFact.EMPTYORDER();
        assert set.size()==1 && BaseUtils.hashEquals(this, set.single());
        return this; 
    }

    public ImOrderSet<Object> subOrder(int from, int to) {
        if(from==0 && to == 1)
            return this;
        return SetFact.EMPTYORDER();
    }

    public <M> ImOrderValueMap<Object, M> mapItOrderValues() {
        return new SingletonOrderMap<Object, M>(this);
    }

    public <M> ImRevValueMap<Object, M> mapItOrderRevValues() {
        return new SingletonRevMap<Object, M>(this);
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetValue<M, Object> getter) {
        return SetFact.singletonOrder(getter.getMapValue(this));
    }

    public <M> ImOrderSet<M> mapOrderSetValues(GetIndexValue<M, Object> getter) {
        return SetFact.singletonOrder(getter.getMapValue(0, this));
    }

    public <M> ImOrderSet<M> mapMergeOrderSetValues(GetValue<M, Object> getter) {
        return SetFact.singletonOrder(getter.getMapValue(this));
    }

    public <M> ImOrderMap<Object, M> mapOrderValues(GetStaticValue<M> getter) {
        return MapFact.<Object, M>singletonOrder(this, getter.getMapValue());
    }

    public <M> ImOrderMap<Object, M> mapOrderValues(GetValue<M, Object> getter) {
        return MapFact.<Object, M>singletonOrder(this, getter.getMapValue(this));
    }

    public <M> ImMap<Object, M> mapOrderValues(GetIndexValue<M, Object> getter) {
        return MapFact.<Object, M>singleton(this, getter.getMapValue(0, this));
    }

    public <M> ImMap<Object, M> mapOrderValues(GetIndex<M> getter) {
        return MapFact.<Object, M>singleton(this, getter.getMapValue(0));
    }

    public <M> ImRevMap<Object, M> mapOrderRevValues(GetIndex<M> getter) {
        return MapFact.<Object, M>singletonRev(this, getter.getMapValue(0));
    }

    public <M> ImRevMap<Object, M> mapOrderRevValues(GetIndexValue<M, Object> getter) {
        return MapFact.<Object, M>singletonRev(this, getter.getMapValue(0, this));
    }

    public <M> ImRevMap<M, Object> mapOrderRevKeys(GetIndex<M> getter) {
        return MapFact.<M, Object>singletonRev(getter.getMapValue(0), this);
    }

    public <M> ImRevMap<M, Object> mapOrderRevKeys(GetIndexValue<M, Object> getter) {
        return MapFact.<M, Object>singletonRev(getter.getMapValue(0, this), this);
    }

    public <V> ImOrderMap<Object, V> toOrderMap(V value) {
        return MapFact.<Object, V>singletonOrder(this, value);
    }

    public Object[] toArray(Object[] array) {
        array[0] = this;
        return array;
    }

    public ImCol<Object> getCol() {
        return this;
    }

    public int indexOf(Object key) {
        if(BaseUtils.hashEquals(this, key))
            return 0;
        return -1;
    }

    public ImMap<Integer, Object> toIndexedMap() {
        return MapFact.<Integer, Object>singleton(0, this);
    }

    public ImList<Object> addList(ImList<? extends Object> list) {
        MList<Object> mResult = ListFact.mList(list.size()+1);
        mResult.add(this);
        mResult.addAll(list);
        return mResult.immutableList();
    }

    public ImList<Object> addList(Object element) {
        return ListFact.toList(this, element);
    }

    public ImList<Object> reverseList() {
        return this;
    }

    public ImList<Object> subList(int i, int to) {
        if(i == 0 && to == 1)
            return this;
        return ListFact.EMPTY();
    }

    public <V> ImList<V> mapList(ImMap<? extends Object, ? extends V> imMap) {
        return ListFact.singleton(((ImMap<Object, V>)imMap).get(this));
    }

    public ImOrderSet<Object> toOrderExclSet() {
        return this;
    }

    public ImList<Object> filterList(FunctionSet<Object> filter) {
        if(filter.contains(this))
            return this;
        return ListFact.EMPTY();
    }

    public <M> ImList<M> mapItListValues(GetValue<M, Object> getter) {
        return ListFact.singleton(getter.getMapValue(this));
    }

    public <M> ImList<M> mapListValues(GetIndex<M> getter) {
        return ListFact.singleton(getter.getMapValue(0));
    }

    public <M> ImList<M> mapListValues(GetIndexValue<M, Object> getter) {
        return ListFact.singleton(getter.getMapValue(0, this));
    }

    public <M> ImList<M> mapListValues(GetValue<M, Object> getter) {
        return ListFact.singleton(getter.getMapValue(this));
    }

    public <M> ImMap<M, Object> mapListMapValues(GetIndex<M> getterKey) {
        return MapFact.<M, Object>singleton(getterKey.getMapValue(0), this);
    }

    public <MK, MV> ImMap<MK, MV> mapListKeyValues(GetIndex<MK> getterKey, GetValue<MV, Object> getterValue) {
        return MapFact.singleton(getterKey.getMapValue(0), getterValue.getMapValue(this));
    }

    public <MK, MV> ImRevMap<MK, MV> mapListRevKeyValues(GetIndex<MK> getterKey, GetValue<MV, Object> getterValue) {
        return MapFact.singletonRev(getterKey.getMapValue(0), getterValue.getMapValue(this));
    }

    public String toString(GetIndexValue<String, Object> getter, String delimiter) {
        return getter.getMapValue(0, this);
    }

    public <V> ImList<V> map(ImMap<? extends Object, ? extends V> map) {
        return ListFact.singleton(((ImMap<Object, V>) map).get(this));
    }

    public List<Object> toJavaList() {
        return Collections.<Object>singletonList(this);
    }

    public boolean intersect(ImSet<? extends Object> set) {
        return ((ImSet<Object>)set).contains(this);
    }

    public boolean intersect(FunctionSet<? extends Object> set) {
        return ((FunctionSet<Object>)set).contains(this);
    }

    public boolean disjoint(ImSet<? extends Object> col) {
        return !((ImSet<Object>)col).contains(this);
    }

    public boolean containsAll(ImSet<? extends Object> wheres) {
        if(wheres.isEmpty())
            return true;

        return wheres.size() == 1 && BaseUtils.hashEquals(this, wheres.single());
    }

    public <G> ImMap<G, ImSet<Object>> group(BaseUtils.Group<G, Object> getter) {
        return MapFact.<G, ImSet<Object>>singleton(getter.group(this), this);
    }

    public <V> ImCol<V> map(ImMap<Object, ? extends V> map) {
        return SetFact.singleton(map.get(this));
    }

    public <EV> ImSet<EV> mapRev(ImRevMap<Object, EV> map) {
        return SetFact.singleton(map.get(this));
    }

    public ImSet<Object> merge(ImSet<? extends Object> merge) {
        if(merge.isEmpty())
            return this;
        if(((ImSet<Object>)merge).contains(this))
            return (ImSet<Object>) merge;
        
        MSet<Object> mSet = SetFact.mSet(merge);
        mSet.add(this);
        return mSet.immutable();
    }

    public ImSet<Object> merge(Object element) {
        if(BaseUtils.hashEquals(this, element))
            return this;

        MExclSet<Object> mSet = SetFact.mExclSet(2);
        mSet.exclAdd(element);
        mSet.exclAdd(this);
        return mSet.immutable();
    }

    @Override
    public ImSet<Object> addExcl(ImSet<? extends Object> merge) {
        if(merge.isEmpty())
            return this;

        MExclSet<Object> mSet = SetFact.mExclSet(merge);
        mSet.exclAddAll(this);
        return mSet.immutable();
    }

    public ImSet<Object> addExcl(Object element) {
        MExclSet<Object> mSet = SetFact.mExclSet(2);
        mSet.exclAdd(element);
        mSet.exclAdd(this);
        return mSet.immutable();
    }

    public <M> ImFilterValueMap<Object, M> mapFilterValues() {
        return new FilterValueMap<Object, M>(this.<M>mapItValues());
    }

    public ImSet<Object> filterFn(FunctionSet<Object> filter) {
        if(filter.contains(this))
            return this;
        return SetFact.EMPTY();
    }

    public ImSet<Object> split(FunctionSet<Object> filter, Result<ImSet<Object>> rest) {
        if(filter.contains(this)) {
            rest.set(SetFact.<Object>EMPTY());
            return this;
        }
        rest.set(this);
        return SetFact.EMPTY();
    }

    public ImSet<Object> filter(ImSet<? extends Object> filter) {
        if(((ImSet<Object>)filter).contains(this))
            return this;
        return SetFact.EMPTY();
    }

    public ImSet<Object> remove(ImSet<? extends Object> remove) {
        if(((ImSet<Object>)remove).contains(this))
            return SetFact.EMPTY();
        return this;
    }

    public ImSet<Object> removeIncl(ImSet<? extends Object> remove) {
        if(remove.isEmpty())
            return this;
        
        assert BaseUtils.hashEquals(this, remove.single());
        return SetFact.EMPTY();
    }

    public ImSet<Object> removeIncl(Object element) {
        assert BaseUtils.hashEquals(this, element);
        return SetFact.EMPTY();
    }

    public <V> ImMap<Object, V> toMap(V value) {
        return MapFact.<Object, V>singleton(this, value);
    }

    public ImMap<Object, Object> toMap() {
        return MapFact.<Object, Object>singleton(this, this);
    }

    public ImRevMap<Object, Object> toRevMap() {
        return MapFact.<Object, Object>singletonRev(this, this);
    }

    public ImOrderSet<Object> toOrderSet() {
        return this;
    }

    public ImOrderSet<Object> sort() {
        return this;
    }

    public <M> ImMap<Object, M> mapItValues(GetValue<M, Object> getter) {
        return MapFact.<Object, M>singleton(this, getter.getMapValue(this));
    }

    public <M> ImSet<M> mapItSetValues(GetValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(this));
    }

    public <M> ImSet<M> mapSetValues(GetValue<M, Object> getter) {
        return SetFact.singleton(getter.getMapValue(this));
    }

    public <M> ImMap<Object, M> mapValues(GetStaticValue<M> getter) {
        return MapFact.<Object, M>singleton(this, getter.getMapValue());
    }

    public <M> ImMap<Object, M> mapValues(GetIndex<M> getter) {
        return MapFact.<Object, M>singleton(this, getter.getMapValue(0));
    }

    public <M> ImMap<Object, M> mapValues(GetValue<M, Object> getter) {
        return MapFact.<Object, M>singleton(this, getter.getMapValue(this));
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(GetValue<MK, Object> getterKey, GetValue<MV, Object> getterValue) {
        return MapFact.singleton(getterKey.getMapValue(this), getterValue.getMapValue(this));
    }

    public <M> ImRevMap<Object, M> mapRevValues(GetIndex<M> getter) {
        return MapFact.<Object, M>singletonRev(this, getter.getMapValue(0));
    }

    public <M> ImRevMap<Object, M> mapRevValues(GetIndexValue<M, Object> getter) {
        return MapFact.<Object, M>singletonRev(this, getter.getMapValue(0, this));
    }

    public <M> ImRevMap<Object, M> mapRevValues(GetStaticValue<M> getter) {
        return MapFact.<Object, M>singletonRev(this, getter.getMapValue());
    }

    public <M> ImRevMap<Object, M> mapRevValues(GetValue<M, Object> getter) {
        return MapFact.<Object, M>singletonRev(this, getter.getMapValue(this));
    }

    public <M> ImRevMap<M, Object> mapRevKeys(GetStaticValue<M> getter) {
        return MapFact.<M, Object>singletonRev(getter.getMapValue(), this);
    }

    public <M> ImRevMap<M, Object> mapRevKeys(GetValue<M, Object> getter) {
        return MapFact.<M, Object>singletonRev(getter.getMapValue(this), this);
    }

    public <M> ImRevMap<M, Object> mapRevKeys(GetIndex<M> getter) {
        return MapFact.<M, Object>singletonRev(getter.getMapValue(0), this);
    }

    public Set<Object> toJavaSet() {
        return Collections.<Object>singleton(this);
    }

    public int size() {
        return 1;
    }

    public Object get(int i) {
        assert i==0;
        return this; 
    }

    protected final boolean equalAsCol(Object obj) {
        if(obj instanceof ImCol)
            return ((ImCol)obj).size()==1 && equals(((ImCol)obj).single());
        if(obj instanceof ImList)
            return ((ImList)obj).size()==1 && equals(((ImList)obj).single());
        return false;
    }
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ImmutableObject)
            return this == obj;
        return equalAsCol(obj);
    }

    public int immutableHashCode() {
        return System.identityHashCode(this);
    }*/
}

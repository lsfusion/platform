package lsfusion.base.col.implementations.simple;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.stored.StoredArraySerializer;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.lambda.set.FunctionSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

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

    public ImCol<K> addCol(K element) {
        MCol<K> mResult = ListFact.mCol(2);
        mResult.add(key);
        mResult.add(element);
        return mResult.immutableCol();
    }

    public <M> ImValueMap<K, M> mapItValues() {
        return new SingletonRevMap<>(key);
    }

    public <M> ImRevValueMap<K, M> mapItRevValues() {
        return new SingletonRevMap<>(key);
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

    @Override
    public K getIdentIncl(K element) {
        assert contains(element);
        return key;
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
        return MapFact.singletonRev(key, 1);
    }

    public <M> ImCol<M> mapColValues(IntObjectFunction<K, M> getter) {
        return SetFact.singleton(getter.apply(0, key));
    }

    public <M> ImCol<M> mapColValues(Function<K, M> getter) {
        return SetFact.singleton(getter.apply(key));
    }

    public <M> ImSet<M> mapColSetValues(IntObjectFunction<K, M> getter) {
        return SetFact.singleton(getter.apply(0, key));
    }

    public <M> ImSet<M> mapColSetValues(Function<K, M> getter) {
        return SetFact.singleton(getter.apply(key));
    }

    public <M> ImSet<M> mapMergeSetValues(Function<K, M> getter) {
        return SetFact.singleton(getter.apply(key));
    }

    public <M> ImSet<M> mapMergeSetSetValues(Function<K, ImSet<M>> getter) {
        return getter.apply(key);
    }

    public <M> ImMap<M, K> mapColKeys(IntFunction<M> getter) {
        return MapFact.<M, K>singleton(getter.apply(0), key);
    }

    public String toString(String separator) {
        return key.toString();
    }

    public String toString(Function<K, String> getter, String delimiter) {
        return getter.apply(key);
    }

    public String toString(Supplier<String> getter, String delimiter) {
        return getter.get();
    }

    public ImList<K> sort(Comparator<K> comparator) {
        return this;
    }

    public Collection<K> toJavaCol() {
        return Collections.singleton(key);
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
        return MapFact.singletonRev(key, set.get(0));
    }

    public <V> ImMap<K, V> mapList(ImList<? extends V> list) {
        return MapFact.singleton(key, list.get(0));
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
        return MapFact.singletonOrder(key, map.get(key));
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
        return new SingletonOrderMap<>(key);
    }

    public <M> ImRevValueMap<K, M> mapItOrderRevValues() {
        return new SingletonRevMap<>(key);
    }

    public <M> ImOrderSet<M> mapOrderSetValues(Function<K, M> getter) {
        return SetFact.singletonOrder(getter.apply(key));
    }

    public <M> ImOrderSet<M> mapOrderSetValues(IntObjectFunction<K, M> getter) {
        return SetFact.singletonOrder(getter.apply(0, key));
    }

    public <M> ImOrderSet<M> mapMergeOrderSetValues(Function<K, M> getter) {
        return SetFact.singletonOrder(getter.apply(key));
    }

    public <M> ImOrderMap<K, M> mapOrderValues(Supplier<M> getter) {
        return MapFact.<K, M>singletonOrder(key, getter.get());
    }

    @Override
    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapOrderValuesEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2 {
        return MapFact.<K, M>singleton(key, getter.apply(key));
    }

    @Override
    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapOrderValuesEx(ThrowingIntObjectFunction<K, M, E1, E2> getter) throws E1, E2 {
        return MapFact.<K, M>singleton(key, getter.apply(0, key));
    }

    @Override
    public <MK, MV, E1 extends Exception, E2 extends Exception> ImMap<MK, MV> mapOrderKeyValuesEx(ThrowingIntObjectFunction<K, MK, E1, E2> getterKey, IntFunction<MV> getterValue) throws E1, E2 {
        return MapFact.singleton(getterKey.apply(0, key), getterValue.apply(0));
    }

    @Override
    public <M> ImOrderMap<M, K> mapOrderKeys(Function<K, M> getter) {
        return MapFact.<M, K>singletonOrder(getter.apply(key), key);
    }

    public <M> ImOrderMap<K, M> mapOrderValues(Function<K, M> getter) {
        return MapFact.<K, M>singletonOrder(key, getter.apply(key));
    }

    public <MK, MV> ImOrderMap<MK, MV> mapOrderKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        return MapFact.singletonOrder(getterKey.apply(key), getterValue.apply(key));
    }

    public <M> ImMap<K, M> mapOrderValues(IntObjectFunction<K, M> getter) {
        return MapFact.<K, M>singleton(key, getter.apply(0, key));
    }

    @Override
    public <M> ImOrderMap<K, M> mapOrderIntValues(IntFunction<M> getter) {
        return MapFact.<K, M>singletonOrder(key, getter.apply(0));
    }

    public <M> ImMap<K, M> mapOrderValues(IntFunction<M> getter) {
        return MapFact.<K, M>singleton(key, getter.apply(0));
    }

    public <M> ImRevMap<K, M> mapOrderRevValues(IntFunction<M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.apply(0));
    }

    public <M> ImRevMap<K, M> mapOrderRevValues(IntObjectFunction<K, M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.apply(0, key));
    }

    public <M> ImRevMap<M, K> mapOrderRevKeys(IntFunction<M> getter) {
        return MapFact.<M, K>singletonRev(getter.apply(0), key);
    }

    public <M> ImRevMap<M, K> mapOrderRevKeys(IntObjectFunction<K, M> getter) {
        return MapFact.<M, K>singletonRev(getter.apply(0, key), key);
    }

    public <V> ImOrderMap<K, V> toOrderMap(V value) {
        return MapFact.singletonOrder(key, value);
    }

    public K[] toArray(K[] array) {
        array[0] = key;
        return array;
    }

    public ImCol<K> getCol() {
        return this;
    }

    public int indexOf(K key) {
        if(BaseUtils.hashEquals(this.key, key))
            return 0;
        return -1;
    }

    @Override
    public boolean containsNull() {
        return this.key == null;
    }

    public ImRevMap<Integer, K> toIndexedMap() {
        return MapFact.singletonRev(0, key);
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

    @Override
    public ImList<K> remove(int i) {
        assert i==0;
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

    public <M> ImList<M> mapItListValues(Function<K, M> getter) {
        return ListFact.singleton(getter.apply(key));
    }

    public <M> ImList<M> mapListValues(IntObjectFunction<K, M> getter) {
        return ListFact.singleton(getter.apply(0, key));
    }

    public <M> ImList<M> mapListValues(Function<K, M> getter) {
        return ListFact.singleton(getter.apply(key));
    }

    public <M> ImMap<M, K> mapListMapValues(IntFunction<M> getterKey) {
        return MapFact.<M, K>singleton(getterKey.apply(0), key);
    }

    public <MK, MV> ImMap<MK, MV> mapListKeyValues(IntFunction<MK> getterKey, Function<K, MV> getterValue) {
        return MapFact.singleton(getterKey.apply(0), getterValue.apply(key));
    }

    public <MK, MV> ImRevMap<MK, MV> mapListRevKeyValues(IntFunction<MK> getterKey, Function<K, MV> getterValue) {
        return MapFact.singletonRev(getterKey.apply(0), getterValue.apply(key));
    }

    public String toString(IntObjectFunction<K, String> getter, String delimiter) {
        return getter.apply(0, key);
    }

    public List<K> toJavaList() {
        return Collections.singletonList(key);
    }

    public boolean intersect(ImSet<? extends K> set) {
        return ((ImSet<K>)set).contains(key);
    }

    public boolean intersectFn(FunctionSet<K> set) {
        return set.contains(key);
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
        G group = getter.group(key);
        if(group==null)
            return MapFact.EMPTY();
        
        return MapFact.singleton(group, this);
    }

    public <G> ImMap<G, ImOrderSet<K>> groupOrder(BaseUtils.Group<G, K> getter) {
        G group = getter.group(key);
        if(group==null)
            return MapFact.EMPTY();

        return MapFact.singleton(group, this);
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
        return new FilterValueMap<>(this.mapItValues());
    }
    public <M> ImFilterRevValueMap<K, M> mapFilterRevValues() {
        return new FilterRevValueMap<>(this.mapItRevValues());
    }

    public ImSet<K> filterFn(FunctionSet<K> filter) {
        if(filter.contains(key))
            return this;
        return SetFact.EMPTY();
    }

    @Override
    public <E1 extends Exception, E2 extends Exception> ImSet<K> filterFnEx(ThrowingPredicate<K, E1, E2> filter) throws E1, E2 {
        if(filter.test(key))
            return this;
        return SetFact.EMPTY();
    }

    public ImSet<K> split(FunctionSet<K> filter, Result<ImSet<K>> rest) {
        if(filter.contains(key)) {
            rest.set(SetFact.EMPTY());
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

    @Override
    public ImOrderSet<K> removeOrderIncl(ImSet<? extends K> remove) {
        if(remove.isEmpty())
            return this;
        assert BaseUtils.hashEquals(key, remove.single());
        return SetFact.EMPTYORDER();
    }

    public ImSet<K> removeIncl(K element) {
        assert BaseUtils.hashEquals(key, element);
        return SetFact.EMPTY();
    }

    @Override
    public <G> ImMap<G, ImList<K>> groupList(BaseUtils.Group<G, K> getter) {
        G group = getter.group(key);
        if(group==null)
            return MapFact.EMPTY();

        return MapFact.singleton(group, this);
    }

    @Override
    public <MK, MV> ImMap<MK, MV> mapListKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        return MapFact.singleton(getterKey.apply(key), getterValue.apply(key));
    }

    public <V> ImMap<K, V> toMap(V value) {
        return MapFact.singleton(key, value);
    }

    public ImMap<K, K> toMap() {
        return MapFact.singleton(key, key);
    }

    public ImRevMap<K, K> toRevMap() {
        return MapFact.singletonRev(key, key);
    }

    public ImOrderSet<K> toOrderSet() {
        return this;
    }

    public ImOrderSet<K> sort() {
        return this;
    }

    public ImOrderSet<K> sortSet(Comparator<K> comparator) {
        return this;
    }

    public <M> ImMap<K, M> mapItValues(Function<K, M> getter) {
        return MapFact.<K, M>singleton(key, getter.apply(key));
    }

    public <M> ImSet<M> mapItSetValues(Function<K, M> getter) {
        return SetFact.singleton(getter.apply(key));
    }

    public <M> ImSet<M> mapSetValues(Function<K, M> getter) {
        return SetFact.singleton(getter.apply(key));
    }

    public <M> ImMap<K, M> mapValues(Supplier<M> getter) {
        return MapFact.<K, M>singleton(key, getter.get());
    }

    @Override
    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapValuesEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2 {
        return MapFact.singleton(key, getter.apply(key));
    }

    public <M> ImMap<K, M> mapValues(IntFunction<M> getter) {
        return MapFact.<K, M>singleton(key, getter.apply(0));
    }

    public <M> ImMap<K, M> mapValues(Function<K, M> getter) {
        return MapFact.<K, M>singleton(key, getter.apply(key));
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        return MapFact.singleton(getterKey.apply(key), getterValue.apply(key));
    }

    public <MK, MV> ImRevMap<MK, MV> mapRevKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue) {
        return MapFact.singletonRev(getterKey.apply(key), getterValue.apply(key));
    }

    public <M> ImRevMap<K, M> mapRevValues(IntFunction<M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.apply(0));
    }

    public <M> ImRevMap<K, M> mapRevValues(IntObjectFunction<K, M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.apply(0, key));
    }

    public <M> ImRevMap<K, M> mapRevValues(Supplier<M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.get());
    }

    public <M> ImRevMap<K, M> mapRevValues(Function<K, M> getter) {
        return MapFact.<K, M>singletonRev(key, getter.apply(key));
    }

    public <M> ImRevMap<M, K> mapRevKeys(Supplier<M> getter) {
        return MapFact.<M, K>singletonRev(getter.get(), key);
    }

    public <M> ImRevMap<M, K> mapRevKeys(Function<K, M> getter) {
        return MapFact.<M, K>singletonRev(getter.apply(key), key);
    }

    public <M> ImRevMap<M, K> mapRevKeys(IntFunction<M> getter) {
        return MapFact.<M, K>singletonRev(getter.apply(0), key);
    }

    public Set<K> toJavaSet() {
        return Collections.singleton(key);
    }

    @Override
    public String toString() {
        return toString(",");
    }

    public K last() {
        return key;
    }

    public ImSet<K> split(ImSet<K> filter, Result<ImSet<K>> rest, Result<ImSet<K>> restSplit) {
        if(filter.contains(key)) {
            restSplit.set(filter.removeIncl(key));
            rest.set(SetFact.EMPTY());
            return this;
        } else {
            restSplit.set(filter);
            rest.set(this);
            return SetFact.EMPTY();
        }
    }

    public <E1 extends Exception, E2 extends Exception> ImOrderSet<K> mapItIdentityOrderValuesEx(ThrowingFunction<K, K, E1, E2> getter) throws E1, E2 {
        K newKey = getter.apply(key);
        if(newKey != key)
            return new SingletonSet<>(newKey);
        return this;
    }

    public static void serialize(Object o, StoredArraySerializer serializer, ByteArrayOutputStream outStream) {
        SingletonSet<?> set = (SingletonSet<?>) o;
        serializer.serialize(set.key, outStream);
    }

    public static Object deserialize(ByteArrayInputStream inStream, StoredArraySerializer serializer) {
        Object key = serializer.deserialize(inStream);
        return new SingletonSet<>(key);
    }
}

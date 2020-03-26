package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.simple.FilterRevValueMap;
import lsfusion.base.col.implementations.simple.FilterValueMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.NotFunctionSet;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class AMap<K, V> extends AColObject implements ImMap<K, V> {

    public String toString() {
        return toString(" - ", ",");
    }

    public String toString(String conc, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getKey(i) + conc + getValue(i));
        }
        return builder.toString();
    }

    public String toString(BiFunction<K, V, String> getter, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for(int i=0,size=size();i<size;i++) {
            if(i!=0)
                builder.append(delimiter);
            builder.append(getter.apply(getKey(i), getValue(i)));
        }
        return builder.toString();
    }

    public Iterable<K> keyIt() {
        return () -> new Iterator<K>() {
            int i=0;

            public boolean hasNext() {
                return i<size();
            }

            public K next() {
                return getKey(i++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterable<V> valueIt() {
        return () -> new Iterator<V>() {
            int i=0;

            public boolean hasNext() {
                return i<size();
            }

            public V next() {
                return getValue(i++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean isEmpty() {
        return size()==0;
    }

    public boolean containsKey(K key) {
        return keys().contains(key);
    }

    public boolean containsValue(V value) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(getValue(i), value))
                return true;
        return false;
    }

    public boolean containsNull() {
        for(int i=0,size=size();i<size;i++)
            if(getValue(i) == null)
                return true;
        return false;
    }

    public ImMap<V, ImSet<K>> groupValues() {
        MExclMap<V, MExclSet<K>> mResult = MapFact.mExclMapMax(size());
        for (int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            V group = getValue(i);
            if(group!=null) {
                MExclSet<K> groupList = mResult.get(group);
                if (groupList == null) {
                    groupList = SetFact.mExclSetMax(size);
                    mResult.exclAdd(group, groupList);
                }
                groupList.exclAdd(key);
            }
        }
        return MapFact.immutable(mResult);
    }

    public ImSet<K> keys() {
        MExclSet<K> mResult = SetFact.mExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getKey(i));
        return mResult.immutable();
    }

    public ImCol<V> values() {
        MCol<V> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getValue(i));
        return mResult.immutableCol();
    }

    public ImRevMap<K, V> toRevMap() {
        MRevMap<V, K> mResult = MapFact.mRevMapMax(size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(!mResult.containsKey(value))
                mResult.revAdd(value, getKey(i));
        }
        return mResult.immutableRev().reverse();
    }

    public ImRevMap<K, V> toRevMap(ImOrderSet<K> interfaces) {
        assert size() == interfaces.size();
        MRevMap<V, K> mResult = MapFact.mRevMapMax(interfaces.size());
        for(int i=0,size=size();i<size;i++) {
            K key = interfaces.get(i);
            V value = get(key);
            if(!mResult.containsKey(value))
                mResult.revAdd(value, key);
        }
        return mResult.immutableRev().reverse();
    }

    public ImRevMap<K, V> toRevExclMap() {
        MRevMap<K, V> mResult = MapFact.mRevMapMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getKey(i), getValue(i));
        return mResult.immutableRev();
    }

    public ImOrderMap<K, V> toOrderMap() {
        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getKey(i), getValue(i));
        return mResult.immutableOrder();
    }

    public V get(K key) {
        return getObject(key);
    }

    public V getPartial(K key) {
        return getObject(key);
    }

    public V getObject(Object key) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(getKey(i), key))
                return getValue(i);
        return null;
    }

    public K singleKey() {
        assert size()==1;
        return getKey(0);
    }

    public V singleValue() {
        assert size()==1;
        return getValue(0);
    }

    public boolean identity() {
        for(int i=0,size=size();i<size;i++)
            if(!BaseUtils.hashEquals(getKey(i), getValue(i)))
                return false;
        return true;
    }

    public ImMap<K, V> merge(ImMap<? extends K, ? extends V> imMap, AddValue<K, V> add) {
        if(imMap.isEmpty()) return this;
        
        if(add.reversed() && size() < imMap.size()) return ((ImMap<K, V>)imMap).merge(this, add.reverse());

        MMap<K, V> mResult = MapFact.mMap(this, add);
        if(!mResult.addAll(imMap))
            return null;
        return mResult.immutable();
    }

    public ImMap<K, V> addExcl(K key, V value) {
        MExclMap<K, V> mResult = MapFact.mExclMap(this);
        mResult.exclAdd(key, value);
        return mResult.immutable();
    }

    public ImMap<K, V> addIfNotContains(K key, V value) {
        if(!containsKey(key))
            return addExcl(key, value);
        return this;
    }

    public ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> imMap) {
        if(imMap.isEmpty()) return this;
        
        if(size() < imMap.size()) return ((ImMap<K,V>)imMap).addExcl(this);

        MExclMap<K, V> mResult = MapFact.mExclMap(this);
        mResult.exclAddAll(imMap);
        return mResult.immutable();
    }

    public ImMap<K, V> addEquals(ImMap<? extends K, ? extends V> imMap) {
        assert keys().containsAll(imMap.keys());

        return ((ImMap<K, V>)imMap).filterFn((key, value) -> BaseUtils.hashEquals(get(key), value));
    }

    @Override
    public ImMap<K, V> removeEquals(ImMap<K, V> map) {
        return filterFn((k, v) -> {
            V rv;
            return (rv = map.get(k)) == null || !BaseUtils.hashEquals(v, rv);         
        });
    }

    public <M> ImMap<K, M> join(ImMap<? super V, M> joinMap) {
        return mapValues(((ImMap<V, M>) joinMap).fnGetValue());
    }

    public <M> ImMap<K, M> rightJoin(ImMap<? extends V, M> joinMap) {
        assert values().toSet().containsAll(joinMap.keys());

        return innerJoin(joinMap);
    }

    public <M> ImMap<K, M> innerJoin(ImMap<? extends V, M> joinMap) {
        MExclMap<K, M> mResult = MapFact.mExclMap(joinMap.size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(((ImMap<V, M>)joinMap).containsKey(value))
                mResult.exclAdd(getKey(i), ((ImMap<V, M>) joinMap).get(value));
        }
        return mResult.immutable();
    }

    public <T> ImMap<K, T> innerCrossValues(ImRevMap<? extends T, ? extends V> imRevMap) {
        return innerJoin(((ImRevMap<T, V>) imRevMap).reverse());
    }

    public ImMap<K, V> filterFn(BiFunction<K, V, Boolean> filter) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.apply(key, value))
                mResult.keep(key, value);
        }
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> filterFn(FunctionSet<K> filter) {
        if(filter.isFull()) // оптимизация
            return this;
        if(filter.isEmpty())
            return MapFact.EMPTY();

        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            if(filter.contains(key))
                mResult.keep(key, getValue(i));
        }
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> filterFnValues(FunctionSet<V> filter) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(filter.contains(value))
                mResult.keep(getKey(i), value);
        }
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> splitKeys(BiFunction<K, V, Boolean> filter, Result<ImMap<K, V>> rest) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        MFilterMap<K, V> mRest = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.apply(key, value))
                mResult.keep(key, value);
            else
                mRest.keep(key, value);
        }
        rest.set(MapFact.imFilter(mRest, this));
        return MapFact.imFilter(mResult, this);
    }

    public ImMap<K, V> splitKeys(FunctionSet<K> filter, Result<ImMap<K, V>> rest) {
        MFilterMap<K, V> mResult = MapFact.mFilter(this);
        MFilterMap<K, V> mRest = MapFact.mFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.contains(key))
                mResult.keep(key, value);
            else
                mRest.keep(key, value);
        }
        rest.set(MapFact.imFilter(mRest, this));
        return MapFact.imFilter(mResult, this);
    }

    public <EK extends K> ImMap<EK, V> filter(ImSet<? extends EK> keys) {
        if(size()<=keys.size())
            return BaseUtils.immutableCast(filterFn(BaseUtils.<FunctionSet<K>>immutableCast(keys)));

        ImFilterValueMap<EK, V> mapFilter = ((ImSet<EK>)keys).mapFilterValues();
        for(int i=0,size=keys.size();i<size;i++) {
            V value = get(keys.get(i));
            if(value!=null)
                mapFilter.mapValue(i, value);
        }
        return mapFilter.immutableValue();
    }

    public <EK extends K> ImMap<EK, V> filterIncl(ImSet<? extends EK> keys) {
        assert keys().containsAll(keys);
        if(keys.size() == size())
            return (ImMap<EK, V>) this;
        
        return ((ImSet<EK>)keys).mapValues(BaseUtils.<Function<EK, V>>immutableCast(fnGetValue()));
    }

    public ImMap<K, V> remove(ImSet<? extends K> keys) {
        if(keys.isEmpty()) // оптимизация
            return this;

        return filterFn(new NotFunctionSet<>((FunctionSet<K>) keys));
    }

    public <EV extends V> ImMap<K, EV> filterValues(ImSet<EV> values) {
        return BaseUtils.immutableCast(filterFnValues(BaseUtils.<FunctionSet<V>>immutableCast(values)));
    }

    public ImMap<K, V> remove(final K remove) {
        return filterFn(element -> !BaseUtils.hashEquals(element, remove));
    }

    public ImMap<K, V> removeIncl(K key) {
        assert containsKey(key);
        return remove(key);
    }

    public ImMap<K, V> removeIncl(ImSet<? extends K> keys) {
        return remove(keys);
    }

    public ImMap<K, V> removeValues(final V value) {
        return filterFnValues(element -> !BaseUtils.hashEquals(element, value));
    }

    public ImMap<K, V> removeNulls() {
        return filterFnValues(element -> element != null);
    }

    public ImMap<K, V> mergeEqualsIncl(final ImMap<K, V> full) {
        return filterFn((key, value) -> BaseUtils.hashEquals(full.get(key), value));
    }

    public ImMap<K, V> mergeEquals(final ImMap<K, V> map) {
        return filterFn((key, value) -> {
            V mapValue = map.get(key);
            return mapValue != null && BaseUtils.hashEquals(mapValue, value);
        });
    }

    public ImMap<K, V> replaceValues(final V value) {
        return mapValues(() -> value);
    }

    public ImMap<K, V> override(K key, V value) {
        if(containsKey(key)) // оптимизация
            return replaceValue(key, value);
        else
            return addExcl(key, value);
    }

    public ImMap<K, V> replaceValue(final K replaceKey, final V replaceValue) {
        return mapValues((key, value) -> BaseUtils.hashEquals(key, replaceKey) ? replaceValue : value);
    }

    public ImMap<K, V> replaceValues(final ImMap<? extends V, ? extends V> map) {
        return mapValues(value -> {
            V mapValue = ((ImMap<V, V>) map).get(value);
            return mapValue != null ? mapValue : value;
        });
    }

    public ImMap<K, V> override(ImMap<? extends K, ? extends V> imMap) {
        return merge(imMap, MapFact.override());
    }
    
    public ImMap<K, V> overrideIncl(final ImMap<? extends K, ? extends V> map) {
        return mapValues((key, value) -> {
            V mapValue = ((ImMap<K, V>) map).get(key);
            return mapValue != null ? mapValue : value;
        });
    }

    public <M> ImMap<K, M> mapItValues(Function<V, M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getValue(i)));
        return mvResult.immutableValue();
    }

    public <M> ImMap<K, M> mapItValues(BiFunction<K, V, M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getKey(i), getValue(i)));
        return mvResult.immutableValue();
    }

    public <E1 extends Exception, E2 extends Exception> ImMap<K, V> mapItIdentityValuesEx(ThrowingFunction<V, V, E1, E2> getter) throws E1, E2 {
        ImValueMap<K, V> mvResult = null;
        for(int i=0,size=size();i<size;i++) {
            V oldValue = getValue(i);
            V newValue = getter.apply(oldValue);
            if (mvResult == null && oldValue != newValue) {
                mvResult = mapItValues();
                for(int j=0;j<i;j++)
                    mvResult.mapValue(j, getValue(j));
            }
            if(mvResult != null)
                mvResult.mapValue(i, newValue);
        }
        if(mvResult != null)
            return mvResult.immutableValue();
        else
            return this;
    }

    public <M> ImMap<K, M> mapValues(Function<V, M> getter) {
        return mapItValues(getter);
    }

    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapItValuesEx(ThrowingFunction<V, M, E1, E2> getter) throws E1, E2{
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getValue(i)));
        return mvResult.immutableValue();
    }

    @Override
    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapValuesEx(ThrowingFunction<V, M, E1, E2> getter) throws E1, E2 {
        return mapItValuesEx(getter);
    }

    public <M> ImMap<K, M> mapKeyValues(Function<K, M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getKey(i)));
        return mvResult.immutableValue();
    }

    public <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapKeyValuesEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2{
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getKey(i)));
        return mvResult.immutableValue();
    }

    public <M> ImSet<M> mapMergeSetValues(BiFunction<K, V, M> getter) {
        MSet<M> mResult = SetFact.mSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(getKey(i), getValue(i)));
        return mResult.immutable();
    }

    public <M> ImSet<M> mapSetValues(BiFunction<K, V, M> getter) {
        MExclSet<M> mResult = SetFact.mExclSetMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(getKey(i), getValue(i)));
        return mResult.immutable();
    }

    public <M> ImMap<K, M> mapValues(Supplier<M> getter) {
        ImValueMap<K, M> mvResult = mapItValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.get());
        return mvResult.immutableValue();
    }

    public <M> ImMap<K, M> mapValues(BiFunction<K, V, M> getter) {
        return mapItValues(getter);
    }

    public <M> ImRevMap<K, M> mapRevValues(IntFunction<M> getter) {
        ImRevValueMap<K, M> mvResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(i));
        return mvResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(Function<V, M> getter) {
        ImRevValueMap<K, M> mvResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getValue(i)));
        return mvResult.immutableValueRev();
    }

    public <M> ImRevMap<K, M> mapRevValues(BiFunction<K, V, M> getter) {
        ImRevValueMap<K, M> mvResult = mapItRevValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getKey(i), getValue(i)));
        return mvResult.immutableValueRev();
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(Function<K, MK> getterKey, Function<V, MV> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getterKey.apply(getKey(i)), getterValue.apply(getValue(i)));
        return mResult.immutable();
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(Function<K, MK> getterKey, BiFunction<K, V, MV> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            mResult.exclAdd(getterKey.apply(key), getterValue.apply(key, getValue(i)));
        }
        return mResult.immutable();
    }

    public <MK, MV> ImMap<MK, MV> mapKeyValues(BiFunction<K, V, MK> getterKey, BiFunction<K, V, MV> getterValue) {
        MExclMap<MK, MV> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            V value = getValue(i);
            mResult.exclAdd(getterKey.apply(key, value), getterValue.apply(key, value));
        }
        return mResult.immutable();
    }

    public <M> ImMap<M, V> mapKeys(Function<K, M> getter) {
        MExclMap<M, V> mResult = MapFact.mExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(getKey(i)), getValue(i));
        return mResult.immutable();
    }

    public <M> ImCol<M> mapColValues(BiFunction<K, V, M> getter) {
        MCol<M> mResult = ListFact.mCol(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(getKey(i), getValue(i)));
        return mResult.immutableCol();
    }

    public ImMap<K, V> mapAddValues(final ImMap<K, V> map, final AddValue<K, V> addValue) {
        return mapValues((key, value) -> addValue.addValue(key, value, map.get(key)));
    }

    public ImOrderMap<K, V> sort(Comparator<K> comparator) { // можно indexes с перегруженным comparator'ом делать
        return keys().sort(comparator).toOrderExclSet().mapOrderMap(this);
    }

    public ImOrderMap<K, V> sort() {
        return keys().sort().mapOrderMap(this);
    }

    public Map<K, V> toJavaMap() {
        Map<K, V> result = new HashMap<>();
        for(int i=0,size=size();i<size;i++)
            result.put(getKey(i), getValue(i));
        return result;
    }

    public Function<K, V> fnGetValue() {
        return this::get;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImMap)) return false;

        ImMap<K, V> map = (ImMap) obj;
        if (map.size() != size()) return false;

        return twins(map);
    }

    protected boolean twins(ImMap<K, V> map) { // assert что size одинаковый
        for (int i = 0; i < size(); i++) {
            K key = getKey(i);
            V value = getValue(i);

            V mapValue = map.get(key);
            
            // proceeding null values
            if (mapValue == null) {
                if(value != null)
                    return false;
                
                if(map.containsKey(key)) // really null value
                    continue;
                else
                    return false;
            } else
                if(value == null)
                    return false;            
            
            if (!mapValue.equals(value)) return false;
        }
        return true;
    }

    public int immutableHashCode() {
        int hash = 0;
        for (int i = 0, size = size(); i < size; i++)
            hash += getKey(i).hashCode() ^ BaseUtils.nullHash(getValue(i));
        return hash;
    }

    public <M> ImFilterValueMap<K, M> mapFilterValues() {
        return new FilterValueMap<>(this.mapItValues());
    }
    public <M> ImFilterRevValueMap<K, M> mapFilterRevValues() {
        return new FilterRevValueMap<>(this.mapItRevValues());
    }

    private static final AddValue<Object, ImMap<Object, ImSet<Object>>> addMergeMapSets = new SymmAddValue<Object, ImMap<Object, ImSet<Object>>>() {
        public ImMap<Object, ImSet<Object>> addValue(Object key, ImMap<Object, ImSet<Object>> prevValue, ImMap<Object, ImSet<Object>> newValue) {
            return prevValue.merge(newValue, ASet.addMergeSet());
        }
    };
    public static <K, KV, V> AddValue<K, ImMap<KV, ImSet<V>>> addMergeMapSets() {
        return BaseUtils.immutableCast(addMergeMapSets);
    }
}

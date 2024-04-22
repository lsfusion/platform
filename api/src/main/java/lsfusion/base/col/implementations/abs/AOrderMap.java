package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.NotFunctionSet;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class AOrderMap<K, V> extends AColObject implements ImOrderMap<K, V> {

    // дублирует AMap, так как там порядок может меняться
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

    public V get(K key) {
        return getMap().get(key);
    }

    public ImSet<K> keys() {
        return getMap().keys();
    }

    public ImCol<V> values() {
        return getMap().values();
    }

    public boolean isEmpty() {
        return getMap().isEmpty();
    }

    public boolean containsKey(K key) {
        return getMap().containsKey(key);
    }

    public V singleValue() {
        return getMap().singleValue();
    }

    public K singleKey() {
        return getMap().singleKey();
    }



    public ImOrderMap<K, V> moveStart(ImSet<K> col) {
        return filterOrder(col).mergeOrder(this);
    }

    public boolean starts(ImSet<K> col) {
        return equals(moveStart(col));
    }

    public <M> ImOrderMap<M, V> mapMergeItOrderKeys(Function<K, M> getter) {
        MOrderMap<M, V> mResult = MapFact.mOrderMapMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(getKey(i)), getValue(i));
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<M, V> mapMergeOrderKeys(Function<K, M> getter) {
        return mapMergeItOrderKeys(getter);
    }

    public <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapMergeItOrderKeysEx(ThrowingFunction<K, M, E1, E2> getter) throws E2, E1 {
        MOrderMap<M, V> mResult = MapFact.mOrderMapMax(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(getKey(i)), getValue(i));
        return mResult.immutableOrder();
    }

    @Override
    public <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapMergeOrderKeysEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2 {
        return mapMergeItOrderKeysEx(getter);
    }

    public <M> ImOrderSet<M> mapOrderSetValues(BiFunction<K, V, M> getter) {
        MOrderExclSet<M> mResult = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(getKey(i), getValue(i)));
        return mResult.immutableOrder();
    }

    public <M> ImList<M> mapListValues(Function<V, M> getter) {
        MList<M> mResult = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mResult.add(getter.apply(getValue(i)));
        return mResult.immutableList();
    }

    public <MK, MV> ImOrderMap<MK, MV> mapOrderKeyValues(BiFunction<K, V, MK> getterKey, Function<V, MV> getterValue) {
        MOrderExclMap<MK, MV> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            mResult.exclAdd(getterKey.apply(getKey(i), value), getterValue.apply(value));
        }
        return mResult.immutableOrder();
    }

    public <MK, MV> ImOrderMap<MK, MV> mapOrderKeyValues(Function<K, MK> getterKey, Function<V, MV> getterValue) {
        MOrderExclMap<MK, MV> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            mResult.exclAdd(getterKey.apply(getKey(i)), getterValue.apply(value));
        }
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<M, V> mapOrderKeys(Function<K, M> getter) {
        MOrderExclMap<M, V> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(getKey(i)), getValue(i));
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<M, V> mapOrderIntKeys(IntFunction<M> getter) {
        MOrderExclMap<M, V> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(i), getValue(i));
        return mResult.immutableOrder();
    }

    public <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapOrderKeysEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2 {
        MOrderExclMap<M, V> mResult = MapFact.mOrderExclMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.exclAdd(getter.apply(getKey(i)), getValue(i));
        return mResult.immutableOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(Supplier<M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.get());
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(BiFunction<K, V, M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getKey(i), getValue(i)));
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(Function<V, M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(getValue(i)));
        return mvResult.immutableValueOrder();
    }

    public <M> ImOrderMap<K, M> mapOrderValues(IntFunction<M> getter) {
        ImOrderValueMap<K, M> mvResult = mapItOrderValues();
        for(int i=0,size=size();i<size;i++)
            mvResult.mapValue(i, getter.apply(i));
        return mvResult.immutableValueOrder();
    }

    public ImOrderMap<K, V> filterOrder(FunctionSet<K> set) {
        if(set.isEmpty()) // оптимизация
            return MapFact.EMPTYORDER();
        
        MOrderFilterMap<K, V> mResult = MapFact.mOrderFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            if(set.contains(key))
                mResult.keep(key, getValue(i));
        }
        return MapFact.imOrderFilter(mResult, this);
    }

    public ImOrderMap<K, V> removeOrder(ImSet<? extends K> keys) {
        if(keys.isEmpty()) // оптимизация
            return this;

        return filterOrder(new NotFunctionSet<>((FunctionSet<K>) keys));
    }
    public ImOrderMap<K, V> removeOrderIncl(ImSet<? extends K> keys) {
        return removeOrder(keys);
    }
    public ImOrderMap<K, V> removeOrderIncl(K remove) {
        MOrderFilterMap<K, V> mResult = MapFact.mOrderFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            if(!BaseUtils.hashEquals(key, remove))
                mResult.keep(key, getValue(i));
        }
        return MapFact.imOrderFilter(mResult, this);
    }

    public ImOrderSet<K> filterOrderValues(FunctionSet<V> set) {
        MOrderFilterSet<K> mResult = SetFact.mOrderFilter(this);
        for(int i=0,size=size();i<size;i++)
            if(set.contains(getValue(i)))
                mResult.keep(getKey(i));
        return SetFact.imOrderFilter(mResult, this);
    }

    @Override
    public ImOrderMap<K, V> filterOrderValuesMap(FunctionSet<V> set) {
        MOrderFilterMap<K, V> mResult = MapFact.mOrderFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(set.contains(value))
                mResult.keep(getKey(i), value);
        }
        return MapFact.imOrderFilter(mResult, this);
    }

    public ImOrderMap<K, V> replaceValues(final V[] values) {
        return mapOrderValues((int i) -> values[i]);
    }

    public ImOrderMap<K, V> replaceValue(final K replaceKey, final V replaceValue) {
        return mapOrderValues((key, value) -> {
            if(BaseUtils.hashEquals(key, replaceKey))
                return replaceValue;
            return value;
        });
    }

    public ImList<V> valuesList() {
        MList<V> mList = ListFact.mList(size());
        for(int i=0,size=size();i<size;i++)
            mList.add(getValue(i));
        return mList.immutableList();
    }

    public ImOrderSet<K> keyOrderSet() {
        MOrderExclSet<K> mList = SetFact.mOrderExclSet(size());
        for(int i=0,size=size();i<size;i++)
            mList.exclAdd(getKey(i));
        return mList.immutableOrder();
    }

    public ImOrderMap<K, V> mergeOrder(ImOrderMap<? extends K, ? extends V> imOrderMap) {
        if(imOrderMap.isEmpty()) return this;

        MOrderMap<K, V> mResult = MapFact.mOrderMap(this);
        mResult.addAll(imOrderMap);
        return mResult.immutableOrder();
    }

    public ImOrderMap<K, V> addOrderExcl(ImOrderMap<? extends K, ? extends V> imOrderMap) {
        if(imOrderMap.isEmpty()) return this;

        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap(this);
        mResult.exclAddAll(imOrderMap);
        return mResult.immutableOrder();
    }

    public ImOrderMap<K, V> addOrderExcl(K key, V value) {
        MOrderExclMap<K, V> mResult = MapFact.mOrderExclMap(this);
        mResult.exclAdd(key, value);
        return mResult.immutableOrder();
    }


    public <M> ImOrderMap<M, V> map(ImMap<K, M> map) {
        return mapMergeOrderKeys(map.fnGetValue());
    }

    public <M> ImOrderMap<M, V> map(ImRevMap<K, M> map) {
        return mapOrderKeys(map.fnGetValue());
    }

    public <G> ImMap<G, ImOrderMap<K, V>> groupOrder(BaseUtils.Group<G, K> getter) {
        MExclMap<G, MOrderExclMap<K, V>> mResult = MapFact.mExclMapMax(size());
        for (int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            G group = getter.group(key);

            MOrderExclMap<K, V> groupList = mResult.get(group);
            if (groupList == null) {
                groupList = MapFact.mOrderExclMap(size);
                mResult.exclAdd(group, groupList);
            }
            groupList.exclAdd(key, getValue(i));
        }
        return MapFact.immutableOrder(mResult);
    }
    public ImOrderMap<V, ImOrderSet<K>> groupOrderValues() {
        MOrderExclMap<V, MOrderExclSet<K>> mResult = MapFact.mOrderExclMap(size());
        for (int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            V group = getValue(i);
            if(group!=null) {
                MOrderExclSet<K> groupList = mResult.get(group);
                if (groupList == null) {
                    groupList = SetFact.mOrderExclSetMax(size);
                    mResult.exclAdd(group, groupList);
                }
                groupList.exclAdd(key);
            }
        }
        return MapFact.immutableOrder(mResult);
    }


    public ImOrderMap<K, V> reverseOrder() {
        return keyOrderSet().reverseList().toOrderExclSet().mapOrderValues(getMap().fnGetValue());
    }

    public int indexOf(K key) {
        for(int i=0,size=size();i<size;i++)
            if(BaseUtils.hashEquals(getKey(i), key))
                return i;
        return -1;
    }

    public int immutableHashCode() {
        int hashCode = 1;
        for (int i=0,size=size();i<size;i++)
            hashCode = 31 * hashCode + (getKey(i).hashCode() ^ BaseUtils.nullHash(getValue(i)));
        return hashCode;
    }

    // собсно ради этого метода класс и создавался
    @Override
    public boolean equals(Object obj) {

        if(this==obj) return true;
        if(!(obj instanceof ImOrderMap)) return false;

        ImOrderMap<K, V> list = (ImOrderMap<K, V>)obj;
        if(list.size()!=size()) return false;

        for(int i=0,size=size();i<size;i++)
            if(!(BaseUtils.hashEquals(getKey(i), list.getKey(i)) && BaseUtils.nullEquals(getValue(i), list.getValue(i))))
                return false;
        return true;
    }

    public ImOrderMap<K, V> removeOrderNulls() {
        return filterOrderValuesMap(element -> element != null);
    }
}

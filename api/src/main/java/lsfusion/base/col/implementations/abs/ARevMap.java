package lsfusion.base.col.implementations.abs;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MFilterRevMap;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.NotFunctionSet;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

// вся реализация в AMap (для множественного наследования)
public abstract class ARevMap<K, V> extends AMap<K, V> implements ImRevMap<K, V> {

    public ImRevMap<V, K> reverse() {
        MRevMap<V, K> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getValue(i), getKey(i));
        return mResult.immutableRev();
    }

    public ImRevMap<K, V> addRevExcl(ImRevMap<? extends K, ? extends V> imRevMap) {
        if(imRevMap.isEmpty()) return this;

        if(size()<imRevMap.size()) return ((ImRevMap<K,V>)imRevMap).addRevExcl(this);

        MRevMap<K, V> mResult = MapFact.mRevMap(this);
        mResult.revAddAll(imRevMap);
        return mResult.immutableRev();
    }

    public ImRevMap<K, V> addRevExcl(K key, V value) {
        MRevMap<K, V> mResult = MapFact.mRevMap(this);
        mResult.revAdd(key, value);
        return mResult.immutableRev();
    }

    public <M> ImRevMap<K, M> join(ImRevMap<V, M> joinMap) {
        return mapRevValues(joinMap.fnGetValue());
    }

    public <M> ImRevMap<K, M> innerJoin(ImRevMap<? extends V, M> joinMap) {
        MRevMap<K, M> mResult = MapFact.mRevMap(joinMap.size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(((ImRevMap<V, M>)joinMap).containsKey(value))
                mResult.revAdd(getKey(i), ((ImRevMap<V, M>) joinMap).get(value));
        }
        return mResult.immutableRev();
    }

    public <T> ImRevMap<K, T> innerCrossValues(ImRevMap<? extends T, ? extends V> imRevMap) {
        return innerJoin(((ImRevMap<T, V>) imRevMap).reverse());
    }

    public <M> ImRevMap<V, M> innerCrossJoin(ImRevMap<K, M> map) {
        return reverse().innerJoin(map);
    }

    public <M> ImRevMap<K, M> rightJoin(ImRevMap<V, M> joinMap) {
        assert values().toSet().containsAll(joinMap.keys());

        MRevMap<K, M> mResult = MapFact.mRevMap(joinMap.size());
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(joinMap.containsKey(value))
                mResult.revAdd(getKey(i), joinMap.get(value));
        }
        return mResult.immutableRev();
    }

    public <M> ImRevMap<K, M> crossValuesRev(ImRevMap<? extends M, ? extends V> imRevMap) {
        return join(((ImRevMap<M, V>) imRevMap).reverse());
    }

    public <M> ImRevMap<K, M> rightCrossValuesRev(ImRevMap<? extends M, ? extends V> imRevMap) {
        return rightJoin(((ImRevMap<M, V>) imRevMap).reverse());
    }

    public <M> ImMap<V, M> rightCrossJoin(ImMap<K, M> map) {
        return reverse().rightJoin(map);
    }

    public <M> ImMap<V, M> innerCrossJoin(ImMap<K, M> map) {
        return reverse().innerJoin(map);
    }

    public <M> ImRevMap<V, M> crossJoin(ImRevMap<K, M> map) {
        return reverse().join(map);
    }

    public <M> ImMap<V, M> crossJoin(ImMap<K, M> map) {
        return reverse().join(map);
    }

    public ImRevMap<K, V> filterFnRev(FunctionSet<K> filter) {
        MFilterRevMap<K, V> mResult = MapFact.mRevFilter(this);
        for(int i=0,size=size();i<size;i++) {
            K key = getKey(i);
            if(filter.contains(key))
                mResult.revKeep(key, getValue(i));
        }
        return MapFact.imRevFilter(mResult, this);
    }

    public ImRevMap<K, V> filterFnValuesRev(FunctionSet<V> filter) {
        MFilterRevMap<K, V> mResult = MapFact.mRevFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            if(filter.contains(value))
                mResult.revKeep(getKey(i), value);
        }
        return MapFact.imRevFilter(mResult, this);
    }

    public <EK extends K> ImRevMap<EK, V> filterRev(ImSet<? extends EK> keys) {
        return BaseUtils.immutableCast(filterFn(BaseUtils.<FunctionSet<K>>immutableCast(keys)));
    }

    public <EK extends K> ImRevMap<EK, V> filterInclRev(ImSet<? extends EK> keys) {
        assert keys().containsAll(keys);
        return ((ImSet<EK>)keys).mapRevValues(BaseUtils.<Function<EK, V>>immutableCast(fnGetValue()));
    }

    public <EV extends V> ImRevMap<K, EV> filterValuesRev(ImSet<EV> values) {
        return BaseUtils.immutableCast(filterFnValues(BaseUtils.<FunctionSet<V>>immutableCast(values)));
    }

    public <EV extends V> ImRevMap<K, EV> filterInclValuesRev(ImSet<EV> values) {
        return reverse().filterInclRev(values).reverse();
    }

    public ImRevMap<K, V> removeRev(final K remove) {
        return filterFnRev(element -> !BaseUtils.hashEquals(element, remove));
    }

    public ImRevMap<K, V> removeRev(ImSet<? extends K> keys) {
        return filterFnRev(new NotFunctionSet<>((ImSet<K>) keys));
    }

    public ImRevMap<K, V> removeValuesRev(ImSet<? extends V> values) {
        return filterFnValuesRev(new NotFunctionSet<>((ImSet<V>) values));
    }

    public ImRevMap<K, V> removeValuesRev(final V value) {
        return filterFnValuesRev(element -> !BaseUtils.hashEquals(element, value));
    }

    public ImSet<V> valuesSet() {
        return reverse().keys();
    }

    public <M> ImRevMap<M, V> mapRevKeys(IntFunction<M> getter) {
        return reverse().mapRevValues(getter).reverse();
    }

    @Override
    public ImOrderMap<K, V> mapOrder(ImOrderSet<V> map) {
        final ImRevMap<V, K> reversed = reverse();
        return map.mapOrderKeyValues(reversed::get, value -> value);
    }

    public <M> ImRevMap<M, V> mapRevKeys(Function<K, M> getter) {
        return reverse().mapRevValues(getter).reverse();
    }

    public <M> ImRevMap<M, V> mapRevKeys(BiFunction<V, K, M> getter) {
        return reverse().mapRevValues(getter).reverse();
    }

    public <MK, MV> ImRevMap<MK, MV> mapRevKeyValues(Function<K, MK> getterKey, Function<V, MV> getterValue) {
        MRevMap<MK, MV> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getterKey.apply(getKey(i)), getterValue.apply(getValue(i)));
        return mResult.immutableRev();
    }

    public ImRevMap<K, V> splitRevKeys(FunctionSet<K> filter, Result<ImRevMap<K, V>> rest) {
        MFilterRevMap<K, V> mResult = MapFact.mRevFilter(this);
        MFilterRevMap<K, V> mRest = MapFact.mRevFilter(this);
        for(int i=0,size=size();i<size;i++) {
            V value = getValue(i);
            K key = getKey(i);
            if(filter.contains(key))
                mResult.revKeep(key, value);
            else
                mRest.revKeep(key, value);
        }
        rest.set(MapFact.imRevFilter(mRest, this));
        return MapFact.imRevFilter(mResult, this);
    }

    @Override
    public ImRevMap<K, V> toRevExclMap() {
        assert values().toSet().size()==size();
        return this;
    }
}

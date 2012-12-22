package platform.base.col.implementations.abs;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.Result;
import platform.base.col.MapFact;
import platform.base.NotFunctionSet;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MFilterRevMap;
import platform.base.col.interfaces.mutable.MRevMap;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;

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

    public <M> ImRevMap<K, M> rightJoin(ImRevMap<V, M> joinMap) {
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
        return ((ImSet<EK>)keys).mapRevValues(BaseUtils.<GetValue<V, EK>>immutableCast(fnGetValue()));
    }

    public <EV extends V> ImRevMap<K, EV> filterValuesRev(ImSet<EV> values) {
        return BaseUtils.immutableCast(filterFnValues(BaseUtils.<FunctionSet<V>>immutableCast(values)));
    }

    public <EV extends V> ImRevMap<K, EV> filterInclValuesRev(ImSet<EV> values) {
        return reverse().filterInclRev(values).reverse();
    }

    public ImRevMap<K, V> removeRev(ImSet<? extends K> keys) {
        return filterFnRev(new NotFunctionSet<K>((ImSet<K>)keys));
    }

    public ImRevMap<K, V> filterNotValuesRev(ImSet<? extends V> values) {
        return filterFnValuesRev(new NotFunctionSet<V>((ImSet<V>)values));
    }

    public ImSet<V> valuesSet() {
        return reverse().keys();
    }

    public <M> ImRevMap<M, V> mapRevKeys(GetIndex<M> getter) {
        return reverse().mapRevValues(getter).reverse();
    }

    public <M> ImRevMap<M, V> mapRevKeys(GetValue<M, K> getter) {
        return reverse().mapRevValues(getter).reverse();
    }

    public <M> ImRevMap<M, V> mapRevKeys(GetKeyValue<M, V, K> getter) {
        return reverse().mapRevValues(getter).reverse();
    }

    public <MK, MV> ImRevMap<MK, MV> mapRevKeyValues(GetValue<MK, K> getterKey, GetValue<MV, V> getterValue) {
        MRevMap<MK, MV> mResult = MapFact.mRevMap(size());
        for(int i=0,size=size();i<size;i++)
            mResult.revAdd(getterKey.getMapValue(getKey(i)), getterValue.getMapValue(getValue(i)));
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

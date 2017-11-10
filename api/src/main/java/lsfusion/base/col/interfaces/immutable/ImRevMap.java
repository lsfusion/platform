package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.FunctionSet;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

public interface ImRevMap<K,V> extends ImMap<K, V> {
    
    ImRevMap<V, K> reverse();

    boolean containsValue(V value);

    ImRevMap<K,V> addRevExcl(ImRevMap<? extends K, ? extends V> map); //            if(where1.size()>where2.size()) return intersect(where2,where1); // пусть добавляется в большую
    ImRevMap<K, V> addRevExcl(K key, V value);

    <M> ImRevMap<K, M> join(ImRevMap<V, M> joinMap);
    <M> ImRevMap<K, M> rightJoin(ImRevMap<V, M> joinMap);

    <M> ImRevMap<K, M> crossValuesRev(ImRevMap<? extends M, ? extends V> map); // берем левую часть (assert'им что правая есть), правую revert'им
    <M> ImRevMap<K, M> rightCrossValuesRev(ImRevMap<? extends M, ? extends V> map); // берем левую часть (assert'им что правая есть), правую revert'им
    <M> ImMap<V, M> rightCrossJoin(ImMap<K, M> map);
    <M> ImMap<V, M> innerCrossJoin(ImMap<K, M> map);
    <M> ImRevMap<V, M> crossJoin(ImRevMap<K, M> map);
    <M> ImMap<V, M> crossJoin(ImMap<K, M> map);

    ImRevMap<K, V> splitRevKeys(FunctionSet<K> keys, Result<ImRevMap<K, V>> rest);

    ImRevMap<K, V> filterFnRev(FunctionSet<K> filter);
    ImRevMap<K, V> filterFnValuesRev(FunctionSet<V> filter);

    <EK extends K> ImRevMap<EK, V> filterRev(ImSet<? extends EK> keys);
    <EK extends K> ImRevMap<EK, V> filterInclRev(ImSet<? extends EK> keys);
    <EV extends V> ImRevMap<K,EV> filterValuesRev(ImSet<EV> values);
    <EV extends V> ImRevMap<K,EV> filterInclValuesRev(ImSet<EV> values);
    ImRevMap<K,V> removeRev(K key);
    ImRevMap<K,V> removeRev(ImSet<? extends K> keys);
    ImRevMap<K,V> filterNotValuesRev(ImSet<? extends V> values);

    ImSet<V> valuesSet();

    <M> ImRevValueMap<K, M> mapItRevValues();

    <M> ImRevMap<M, V> mapRevKeys(GetIndex<M> getter); // reverse.mapValues.reverse
    <M> ImRevMap<M, V> mapRevKeys(GetValue<M, K> getter); // reverse.mapValues.reverse
    <M> ImRevMap<M, V> mapRevKeys(GetKeyValue<M, V, K> getter); // reverse.mapValues.reverse
    <MK, MV> ImRevMap<MK,MV> mapRevKeyValues(GetValue<MK, K> getterKey, GetValue<MV, V> getterValue);
}

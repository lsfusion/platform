package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;

public interface ImRevMap<K,V> extends ImMap<K, V> {
    
    ImRevMap<V, K> reverse();

    boolean containsValue(V value);

    ImRevMap<K,V> addRevExcl(ImRevMap<? extends K, ? extends V> map); //            if(where1.size()>where2.size()) return intersect(where2,where1); // пусть добавляется в большую
    ImRevMap<K, V> addRevExcl(K key, V value);

    <M> ImRevMap<K, M> join(ImRevMap<V, M> joinMap);
    <M> ImRevMap<K, M> innerJoin(ImRevMap<? extends V, M> map);
    <M> ImRevMap<K, M> rightJoin(ImRevMap<V, M> joinMap);

    <M> ImRevMap<K, M> crossValuesRev(ImRevMap<? extends M, ? extends V> map); // берем левую часть (assert'им что правая есть), правую revert'им
    <M> ImRevMap<K, M> rightCrossValuesRev(ImRevMap<? extends M, ? extends V> map); // берем левую часть (assert'им что правая есть), правую revert'им
    <T> ImRevMap<K, T> innerCrossValues(ImRevMap<? extends T, ? extends V> map); // только те которые есть в обоих map'ах
    <M> ImMap<V, M> rightCrossJoin(ImMap<K, M> map);
    <M> ImRevMap<V, M> innerCrossJoin(ImRevMap<K, M> map);
    <M> ImMap<V, M> innerCrossJoin(ImMap<K, M> map);
    <M> ImRevMap<V, M> crossJoin(ImRevMap<K, M> map);
    <M> ImMap<V, M> crossJoin(ImMap<K, M> map);

    ImRevMap<K, V> splitRevKeys(FunctionSet<K> keys, Result<ImRevMap<K, V>> rest);
    default ImRevMap<K, V> splitRevKeys(SFunctionSet<K> keys, Result<ImRevMap<K, V>> rest) {
        return splitRevKeys((FunctionSet<K>) keys, rest);
    }
    
    ImRevMap<K, V> filterFnRev(FunctionSet<K> filter);
    default ImRevMap<K, V> filterFnRev(SFunctionSet<K> filter) {
        return filterFnRev((FunctionSet<K>) filter);
    }
    
    ImRevMap<K, V> filterFnValuesRev(FunctionSet<V> filter);
    default ImRevMap<K, V> filterFnValuesRev(SFunctionSet<V> filter) {
        return filterFnValuesRev((FunctionSet<V>) filter);
    }
    
    <EK extends K> ImRevMap<EK, V> filterRev(ImSet<? extends EK> keys);
    <EK extends K> ImRevMap<EK, V> filterInclRev(ImSet<? extends EK> keys);
    <EV extends V> ImRevMap<K,EV> filterValuesRev(ImSet<EV> values);
    <EV extends V> ImRevMap<K,EV> filterInclValuesRev(ImSet<EV> values);
    ImRevMap<K,V> removeRev(K key);
    ImRevMap<K,V> removeRev(ImSet<? extends K> keys);
    ImRevMap<K,V> removeValuesRev(ImSet<? extends V> values);
    ImRevMap<K,V> removeValuesRev(V value);

    ImOrderMap<K, V> mapOrder(ImOrderSet<V> map);

    ImSet<V> valuesSet();

    <M> ImRevValueMap<K, M> mapItRevValues();

    <M> ImRevMap<M, V> mapRevKeys(IntFunction<M> getter); // reverse.mapValues.reverse
    <M> ImRevMap<M, V> mapRevKeys(Function<K, M> getter); // reverse.mapValues.reverse
    <M> ImRevMap<M, V> mapRevKeys(BiFunction<V, K, M> getter); // reverse.mapValues.reverse
    <MK, MV> ImRevMap<MK,MV> mapRevKeyValues(Function<K, MK> getterKey, Function<V, MV> getterValue);
}

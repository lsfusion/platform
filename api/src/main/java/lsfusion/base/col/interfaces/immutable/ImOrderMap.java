package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImOrderValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ThrowingFunction;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface ImOrderMap<K,V> {

    int size();
    K getKey(int i);
    V getValue(int i);
    
    V get(K key);
    ImSet<K> keys();
    ImCol<V> values();

    boolean isEmpty();
    boolean containsKey(K key);
    V singleValue();
    K singleKey();

    String toString(String conc, String delimiter);
    String toString(BiFunction<K, V, String> getter, String delimiter);

    Iterable<K> keyIt();
    Iterable<V> valueIt();

    ImMap<K, V> getMap();
    
    int indexOf(K key);
    
    ImOrderMap<K, V> reverseOrder();

    ImOrderMap<K, V> removeOrderNulls();

    <G> ImMap<G, ImOrderMap<K, V>> groupOrder(BaseUtils.Group<G, K> getter);
    ImOrderMap<V, ImOrderSet<K>> groupOrderValues();

    <M> ImOrderMap<M, V> map(ImRevMap<K,M> map);
    <M> ImOrderMap<M, V> map(ImMap<K,M> map); // с повторением

    ImOrderMap<K, V> addOrderExcl(K key, V value);
    ImOrderMap<K, V> addOrderExcl(ImOrderMap<? extends K, ? extends V> map); // не пересекаются

    ImOrderMap<K, V> mergeOrder(ImOrderMap<? extends K, ? extends V> map);

    ImOrderSet<K> keyOrderSet();
    ImList<V> valuesList();

    ImOrderMap<K, V> replaceValue(K replaceKey, V replaceValue);
    ImOrderMap<K, V> replaceValues(V[] values);

    ImOrderMap<K, V> filterOrder(FunctionSet<K> set);
    default ImOrderMap<K, V> filterOrder(SFunctionSet<K> set) {
        return filterOrder((FunctionSet<K>) set);
    }

    ImOrderSet<K> filterOrderValues(FunctionSet<V> set);
    default ImOrderSet<K> filterOrderValues(SFunctionSet<V> set) {
        return filterOrderValues((FunctionSet<V>) set);
    }
    
    ImOrderMap<K, V> filterOrderValuesMap(FunctionSet<V> set);
    default ImOrderMap<K, V> filterOrderValuesMap(SFunctionSet<V> set) {
        return filterOrderValuesMap((FunctionSet<V>) set);
    }
    
    ImOrderMap<K, V> removeOrder(ImSet<? extends K> keys);
    ImOrderMap<K, V> removeOrderIncl(ImSet<? extends K> keys);
    ImOrderMap<K, V> removeOrderIncl(K remove);

    <M> ImOrderValueMap<K, M> mapItOrderValues();

    <M> ImOrderMap<M, V> mapMergeItOrderKeys(Function<K, M> getter); // с последействием

    <M> ImOrderMap<K,M> mapOrderValues(IntFunction<M> getter);
    <M> ImOrderMap<K,M> mapOrderValues(Function<V, M> getter);
    <M> ImOrderMap<K,M> mapOrderValues(BiFunction<K, V, M> getter);
    <M> ImOrderMap<K,M> mapOrderValues(Supplier<M> getter);
    <M> ImOrderMap<M, V> mapOrderKeys(Function<K, M> getter);
    <MK, MV> ImOrderMap<MK,MV> mapOrderKeyValues(BiFunction<K, V, MK> getterKey, Function<V, MV> getterValue);
    <MK, MV> ImOrderMap<MK,MV> mapOrderKeyValues(Function<K, MK> getterKey, Function<V, MV> getterValue);

    <M> ImOrderSet<M> mapOrderSetValues(BiFunction<K, V, M> getter);

    <M> ImList<M> mapListValues(Function<V, M> getter);

    <M> ImOrderMap<M, V> mapMergeOrderKeys(Function<K, M> getter);

    <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapOrderKeysEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2;
    <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapMergeOrderKeysEx(ThrowingFunction<K, M, E1, E2> getter) throws E1, E2;
    
    boolean starts(ImSet<K> set);
    ImOrderMap<K,V> moveStart(ImSet<K> col);
}

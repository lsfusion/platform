package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.FunctionSet;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.Comparator;
import java.util.Map;

public interface ImMap<K, V> {

    int size();
    K getKey(int i);
    V getValue(int i);

    Iterable<K> keyIt();
    Iterable<V> valueIt();

    boolean isEmpty();

    ImSet<K> keys();
    ImCol<V> values();

    K singleKey();
    V singleValue();

    ImMap<V, ImSet<K>> groupValues();

    ImRevMap<K, V> toRevMap(); // костыль (недетерминированный метод), но там в QueryExpr, заколебешься с generics'ами у GroupExpr делать ImRevMap
    ImRevMap<K, V> toRevExclMap();
    ImOrderMap<K, V> toOrderMap();

    V get(K key);
    V getPartial(K key);
    V getObject(Object key);
    boolean containsKey(K key);
    boolean containsValue(V value);
    public boolean containsNull();

    boolean identity();

    ImMap<K,V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add);
    ImMap<K, V> addExcl(K key, V value);
    ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map); // не пересекаются
    ImMap<K, V> addEquals(ImMap<? extends K, ? extends V> map); // слить если равны, аналог mergeEquals в BaseUtils

    // joins

    <M> ImMap<K, M> join(ImMap<? super V, M> joinMap); // assert что левая включает правую
    <M> ImMap<K, M> rightJoin(ImMap<? extends V, M> joinMap);
    <M> ImMap<K, M> innerJoin(ImMap<? extends V, M> joinMap);
    <T> ImMap<K, T> innerCrossValues(ImRevMap<? extends T, ? extends V> map); // только те которые есть в обоих map'ах

    // filters
    
    <M> ImFilterValueMap<K, M> mapFilterValues();

    ImMap<K, V> filterFn(FunctionSet<K> filter);
    ImMap<K, V> filterFnValues(FunctionSet<V> filter);
    ImMap<K, V> filterFn(GetKeyValue<Boolean, K, V> filter);

    ImMap<K, V> splitKeys(GetKeyValue<Boolean, K, V> keys, Result<ImMap<K, V>> rest);
    ImMap<K, V> splitKeys(FunctionSet<K> keys, Result<ImMap<K, V>> rest);

    <EK extends K> ImMap<EK, V> filter(ImSet<? extends EK> keys);
    <EK extends K> ImMap<EK, V> filterIncl(ImSet<? extends EK> keys);
    <EV extends V> ImMap<K, EV> filterValues(ImSet<EV> values);
    ImMap<K, V> remove(ImSet<? extends K> keys);
    ImMap<K, V> remove(K key);
    ImMap<K, V> removeValues(V value); // желательно не менять если нет
    ImMap<K, V> removeNotEquals(ImMap<K, V> full);

    // replaces

    public ImMap<K, V> replaceValues(V value);
    public ImMap<K, V> override(K key, V value);
    public ImMap<K, V> replaceValue(K key, V value);
    ImMap<K, V> replaceValues(ImMap<? extends V, ? extends V> map);
    ImMap<K,V> override(ImMap<? extends K,? extends V> map); // перекрываем this, значениями из map, replace в BaseUtils !!! тут важно разделить те которые добавляют и нет

    public <M> ImValueMap<K,M> mapItValues();
    public <M> ImRevValueMap<K,M> mapItRevValues();

    <M> ImMap<K,M> mapItValues(GetValue<M,V> getter); // с последействием

    // "функциональщина"
    <M> ImMap<K,M> mapValues(GetValue<M,V> getter);
    <M> ImMap<K,M> mapKeyValues(GetValue<M,K> getter);
    <M> ImMap<K,M> mapValues(GetStaticValue<M> getter);
    <M> ImMap<K,M> mapValues(GetKeyValue<M,K,V> getter);
    <M> ImRevMap<K,M> mapRevValues(GetIndex<M> getter);
    <M> ImRevMap<K,M> mapRevValues(GetValue<M,V> getter);
    <M> ImRevMap<K,M> mapRevValues(GetKeyValue<M,K, V> getter);
    <MK, MV> ImMap<MK,MV> mapKeyValues(GetValue<MK, K> getterKey, GetValue<MV, V> getterValue);

    <M, E1 extends Exception, E2 extends Exception> ImMap<K,M> mapKeyValuesEx(GetExValue<M,K,E1,E2> getter) throws E1, E2;
    <M, E1 extends Exception, E2 extends Exception> ImMap<K,M> mapValuesEx(GetExValue<M,V,E1,E2> getter) throws E1, E2;

    <M> ImSet<M> mapMergeSetValues(GetKeyValue<M, K, V> getter);

    <M> ImMap<M,V> mapKeys(GetValue<M,K> getter);

    <M> ImCol<M> mapColValues(GetKeyValue<M, K, V> getter);

    ImMap<K, V> mapAddValues(ImMap<K, V> map, AddValue<K, V> addValue);

    String toString(String conc, String delimiter);
    String toString(GetKeyValue<String, K, V> getter, String delimiter);

    ImOrderMap<K, V> sort(Comparator<K> comparator);
    ImOrderMap<K, V> sort();

    Map<K, V> toJavaMap();
    
    GetValue<V, K> fnGetValue();
}

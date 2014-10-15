package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

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
    String toString(GetKeyValue<String, K, V> getter, String delimiter);

    Iterable<K> keyIt();
    Iterable<V> valueIt();

    ImMap<K, V> getMap();
    
    int indexOf(K key);
    
    ImOrderMap<K, V> reverseOrder();

    public ImOrderMap<K, V> removeOrderNulls();

    <G> ImMap<G, ImOrderMap<K, V>> groupOrder(BaseUtils.Group<G, K> getter);
    ImOrderMap<V, ImOrderSet<K>> groupOrderValues();

    <M> ImOrderMap<M, V> map(ImRevMap<K,M> map);
    <M> ImOrderMap<M, V> map(ImMap<K,M> map); // с повторением

    ImOrderMap<K, V> addOrderExcl(ImOrderMap<? extends K, ? extends V> map); // не пересекаются

    ImOrderMap<K, V> mergeOrder(ImOrderMap<? extends K, ? extends V> map);

    ImOrderSet<K> keyOrderSet();
    ImList<V> valuesList();

    ImOrderMap<K, V> replaceValues(V[] values);

    ImOrderMap<K, V> filterOrder(FunctionSet<K> set);
    ImOrderSet<K> filterOrderValues(FunctionSet<V> set);
    ImOrderMap<K, V> filterOrderValuesMap(FunctionSet<V> set);

    <M> ImOrderValueMap<K, M> mapItOrderValues();

    <M> ImOrderMap<M, V> mapMergeItOrderKeys(GetValue<M, K> getter); // с последействием

    public <M> ImOrderMap<K,M> mapOrderValues(GetIndex<M> getter);
    public <M> ImOrderMap<K,M> mapOrderValues(GetValue<M, V> getter);
    public <M> ImOrderMap<K,M> mapOrderValues(GetKeyValue<M,K, V> getter);
    public <M> ImOrderMap<K,M> mapOrderValues(GetStaticValue<M> getter);
    public <M> ImOrderMap<M, V> mapOrderKeys(GetValue<M, K> getter);
    <MK, MV> ImOrderMap<MK,MV> mapOrderKeyValues(GetKeyValue<MK, K, V> getterKey, GetValue<MV, V> getterValue);
    <MK, MV> ImOrderMap<MK,MV> mapOrderKeyValues(GetValue<MK, K> getterKey, GetValue<MV, V> getterValue);

    <M> ImOrderSet<M> mapOrderSetValues(GetKeyValue<M, K, V> getter);

    <M> ImList<M> mapListValues(GetValue<M, V> getter);

    public <M> ImOrderMap<M, V> mapMergeOrderKeys(GetValue<M, K> getter);

    public <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapOrderKeysEx(GetExValue<M, K, E1, E2> getter) throws E1, E2;
    public <M, E1 extends Exception, E2 extends Exception> ImOrderMap<M, V> mapMergeOrderKeysEx(GetExValue<M, K, E1, E2> getter) throws E1, E2;
    
    boolean starts(ImSet<K> set);
    public ImOrderMap<K,V> moveStart(ImSet<K> col);
}

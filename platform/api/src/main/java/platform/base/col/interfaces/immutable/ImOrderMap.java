package platform.base.col.interfaces.immutable;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.interfaces.mutable.mapvalue.*;

import java.util.Collection;
import java.util.List;

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
    
    boolean starts(ImSet<K> set);
    public ImOrderMap<K,V> moveStart(ImSet<K> col);
}

package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.List;

public interface ImList<K> extends Iterable<K> {

    int size();
    K get(int i);
    boolean isEmpty();
    K single();

    <G> ImMap<G, ImList<K>> groupList(BaseUtils.Group<G, K> getter);
    
    K[] toArray(K[] array);
    
    ImCol<K> getCol();

    int indexOf(K key);
    ImRevMap<Integer, K> toIndexedMap();

    ImList<K> addList(ImList<? extends K> list);
    ImList<K> addList(K element);

    ImList<K> reverseList();
    ImList<K> subList(int i, int to);

    <V> ImList<V> mapList(ImMap<? extends K, ? extends V> imMap);

    ImOrderSet<K> toOrderSet();
    ImOrderSet<K> toOrderExclSet();

    // фильтрация
    ImList<K> filterList(FunctionSet<K> filter);

    <M> ImList<M> mapItListValues(GetValue<M, K> getter); // с последействием

    <M> ImList<M> mapListValues(GetIndex<M> getter);
    <M> ImList<M> mapListValues(GetIndexValue<M, K> getter);
    <M> ImList<M> mapListValues(GetValue<M, K> getter);

    <M> ImMap<M,K> mapListMapValues(GetIndex<M> getterKey);
    <M> ImRevMap<M,K> mapListRevValues(GetIndex<M> getterKey);
    <MK, MV> ImMap<MK,MV> mapListKeyValues(GetIndex<MK> getterKey, GetValue<MV, K> getterValue);
    <MK, MV> ImMap<MK,MV> mapListKeyValues(GetValue<MK, K> getterKey, GetValue<MV, K> getterValue);
    <MK, MV> ImRevMap<MK,MV> mapListRevKeyValues(GetIndex<MK> getterKey, GetValue<MV, K> getterValue);

    String toString(String separator);
    String toString(GetValue<String, K> getter, String delimiter);
    String toString(GetStaticValue<String> getter, String delimiter);
    String toString(GetIndexValue<String, K> getter, String delimiter);

    List<K> toJavaList();

    K last();
}

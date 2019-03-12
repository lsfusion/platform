package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

public interface ImOrderSet<K> extends ImList<K> {
    
    boolean contains(K element);

    <G> ImMap<G, ImOrderSet<K>> groupOrder(BaseUtils.Group<G, K> getter);

    ImSet<K> getSet();

    ImOrderSet<K> addOrderExcl(ImOrderSet<? extends K> map);
    ImOrderSet<K> addOrderExcl(K element);
    ImOrderSet<K> mergeOrder(ImOrderSet<? extends K> col);
    ImOrderSet<K> mergeOrder(K element);

    <V> ImRevMap<K, V> mapSet(ImOrderSet<? extends V> set);
    <V> ImMap<K, V> mapList(ImList<? extends V> list);

    ImOrderSet<K> removeOrder(ImSet<? extends K> set);
    ImOrderSet<K> removeOrderIncl(ImSet<? extends K> set);
    ImOrderSet<K> removeOrderIncl(K element);

    ImRevMap<Integer, K> toIndexedMap();

    <V> ImOrderSet<V> mapOrder(ImRevMap<? extends K, ? extends V> map);

    <V> ImOrderMap<K, V> mapOrderMap(ImMap<K, V> map);

    ImOrderSet<K> reverseOrder();

    // фильтрация

    ImOrderSet<K> filterOrder(FunctionSet<K> filter);

    ImOrderSet<K> filterOrderIncl(ImSet<? extends K> set);

    ImOrderSet<K> subOrder(int from, int to);

    <M> ImOrderValueMap<K, M> mapItOrderValues();
    <M> ImRevValueMap<K, M> mapItOrderRevValues(); // предполагается заполнение в том же порядке

    <M> ImOrderSet<M> mapOrderSetValues(GetValue<M, K> getter);
    <M> ImOrderSet<M> mapOrderSetValues(GetIndexValue<M, K> getter);
    <M> ImOrderSet<M> mapMergeOrderSetValues(GetValue<M, K> getter);

    <M> ImOrderMap<M, K> mapOrderKeys(GetValue<M, K> getter);
    
    <M> ImOrderMap<K, M> mapOrderValues(GetStaticValue<M> getter);
    <M> ImOrderMap<K, M> mapOrderValues(GetValue<M, K> getter);
    <MK, MV> ImOrderMap<MK,MV> mapOrderKeyValues(GetValue<MK, K> getterKey, GetValue<MV, K> getterValue);

    <M> ImMap<K, M> mapOrderValues(GetIndexValue<M, K> getter); // в порядке order вызывать getter
    <M> ImMap<K, M> mapOrderValues(GetIndex<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<K, M> mapOrderRevValues(GetIndex<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<K, M> mapOrderRevValues(GetIndexValue<M, K> getter); // в порядке order вызывать getter
    <M> ImRevMap<M, K> mapOrderRevKeys(GetIndex<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<M, K> mapOrderRevKeys(GetIndexValue<M, K> getter); // в порядке order вызывать getter

    <V> ImOrderMap<K, V> toOrderMap(V value);

}

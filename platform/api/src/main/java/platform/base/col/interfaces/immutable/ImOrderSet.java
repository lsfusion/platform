package platform.base.col.interfaces.immutable;

import platform.base.BaseUtils;
import platform.base.FunctionSet;
import platform.base.col.interfaces.mutable.mapvalue.*;

import java.util.List;

public interface ImOrderSet<K> extends ImList<K> {

    boolean contains(K element);

    ImSet<K> getSet();

    ImOrderSet<K> addOrderExcl(ImOrderSet<? extends K> map);
    ImOrderSet<K> addOrderExcl(K element);
    ImOrderSet<K> mergeOrder(ImOrderSet<? extends K> col);

    <V> ImRevMap<K, V> mapSet(ImOrderSet<? extends V> set);
    <V> ImMap<K, V> mapList(ImList<? extends V> list);

    ImOrderSet<K> removeOrder(ImSet<? extends K> set);
    ImOrderSet<K> removeOrderIncl(K element);

    <V> ImOrderSet<V> mapOrder(ImRevMap<? extends K, ? extends V> map);
    <V> ImOrderSet<V> mapOrder(ImMap<? extends K, ? extends V> map);

    <V> ImOrderMap<K, V> mapOrderMap(ImMap<K, V> map);

    ImOrderSet<K> reverseOrder();

    // фильтрация

    public ImOrderSet<K> filterOrder(FunctionSet<K> filter);

    ImOrderSet<K> filterOrderIncl(ImSet<? extends K> set);

    ImOrderSet<K> subOrder(int i, int length);

    public <M> ImOrderValueMap<K, M> mapItOrderValues();
    <M> ImRevValueMap<K, M> mapItOrderRevValues(); // предполагается заполнение в том же порядке

    <M> ImOrderSet<M> mapOrderSetValues(GetValue<M, K> getter);
    <M> ImOrderSet<M> mapOrderSetValues(GetIndexValue<M, K> getter);
    <M> ImOrderSet<M> mapMergeOrderSetValues(GetValue<M, K> getter);

    <M> ImOrderMap<K, M> mapOrderValues(GetStaticValue<M> getter);
    <M> ImOrderMap<K, M> mapOrderValues(GetValue<M, K> getter);

    <M> ImMap<K, M> mapOrderValues(GetIndexValue<M, K> getter); // в порядке order вызывать getter
    <M> ImMap<K, M> mapOrderValues(GetIndex<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<K, M> mapOrderRevValues(GetIndex<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<K, M> mapOrderRevValues(GetIndexValue<M, K> getter); // в порядке order вызывать getter
    <M> ImRevMap<M, K> mapOrderRevKeys(GetIndex<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<M, K> mapOrderRevKeys(GetIndexValue<M, K> getter); // в порядке order вызывать getter

    <V> ImOrderMap<K, V> toOrderMap(V value);

}

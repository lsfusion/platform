package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

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
    default ImOrderSet<K> filterOrder(SFunctionSet<K> filter) {
        return filterOrder((FunctionSet<K>) filter);    
    }

    ImOrderSet<K> filterOrderIncl(ImSet<? extends K> set);

    ImOrderSet<K> subOrder(int from, int to);

    <M> ImOrderValueMap<K, M> mapItOrderValues();
    <M> ImRevValueMap<K, M> mapItOrderRevValues(); // предполагается заполнение в том же порядке

    <M> ImOrderSet<M> mapOrderSetValues(Function<K, M> getter);
    <M> ImOrderSet<M> mapOrderSetValues(IntObjectFunction<K, M> getter);
    <M> ImOrderSet<M> mapMergeOrderSetValues(Function<K, M> getter);

    <M> ImOrderMap<M, K> mapOrderKeys(Function<K, M> getter);
    
    <M> ImOrderMap<K, M> mapOrderValues(Supplier<M> getter);
    <M> ImOrderMap<K, M> mapOrderValues(Function<K, M> getter);
    <MK, MV> ImOrderMap<MK,MV> mapOrderKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue);

    <M> ImMap<K, M> mapOrderValues(IntObjectFunction<K, M> getter); // в порядке order вызывать getter
    <M> ImMap<K, M> mapOrderValues(IntFunction<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<K, M> mapOrderRevValues(IntFunction<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<K, M> mapOrderRevValues(IntObjectFunction<K, M> getter); // в порядке order вызывать getter
    <M> ImRevMap<M, K> mapOrderRevKeys(IntFunction<M> getter); // в порядке order вызывать getter
    <M> ImRevMap<M, K> mapOrderRevKeys(IntObjectFunction<K, M> getter); // в порядке order вызывать getter

    <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapOrderValuesEx(ThrowingFunction<K, M, E1,E2> getter) throws E1, E2;
    <M, E1 extends Exception, E2 extends Exception> ImMap<K, M> mapOrderValuesEx(ThrowingIntObjectFunction<K, M, E1,E2> getter) throws E1, E2;
    <MK, MV, E1 extends Exception, E2 extends Exception> ImMap<MK, MV> mapOrderKeyValuesEx(ThrowingIntObjectFunction<K, MK, E1,E2> getterKey, IntFunction<MV> getterValue) throws E1, E2;

    <E1 extends Exception, E2 extends Exception> ImOrderSet<K> mapItIdentityOrderValuesEx(ThrowingFunction<K, K, E1,E2> getter) throws E1, E2; // with aftereffect, identity optimization

    <V> ImOrderMap<K, V> toOrderMap(V value);

}

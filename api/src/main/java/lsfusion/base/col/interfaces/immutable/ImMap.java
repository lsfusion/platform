package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.AddValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.Comparator;
import java.util.Map;
import java.util.function.*;

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
    ImRevMap<K, V> toRevMap(ImOrderSet<K> keys);
    ImRevMap<K, V> toRevExclMap();
    ImOrderMap<K, V> toOrderMap();

    V get(K key);
    V getPartial(K key);
    V getObject(Object key);
    boolean containsKey(K key);
    boolean containsValue(V value);
    boolean containsNull();

    boolean identity();

    ImMap<K,V> merge(ImMap<? extends K, ? extends V> map, AddValue<K, V> add);
    ImMap<K, V> addExcl(K key, V value);
    ImMap<K, V> addIfNotContains(K key, V value);
    ImMap<K, V> addExcl(ImMap<? extends K, ? extends V> map); // не пересекаются
    ImMap<K, V> addEquals(ImMap<? extends K, ? extends V> map); // слить если равны, аналог mergeEquals в BaseUtils
    ImMap<K, V> mergeEqualsIncl(ImMap<K, V> full); // тоже самое что сверху, с переменой параметров
    ImMap<K, V> mergeEquals(ImMap<K, V> map); // тоже самое что сверху, с переменой параметров

    // joins

    <M> ImMap<K, M> join(ImMap<? super V, M> joinMap); // assert что левая включает правую
    <M> ImMap<K, M> rightJoin(ImMap<? extends V, M> joinMap);
    <M> ImMap<K, M> innerJoin(ImMap<? extends V, M> joinMap);
    <T> ImMap<K, T> innerCrossValues(ImRevMap<? extends T, ? extends V> map); // только те которые есть в обоих map'ах

    // filters
    
    <M> ImFilterValueMap<K, M> mapFilterValues();
    <M> ImFilterRevValueMap<K, M> mapFilterRevValues();

    ImMap<K, V> filterFn(FunctionSet<K> filter);
    default ImMap<K, V> filterFn(SFunctionSet<K> filter) {
        return filterFn((FunctionSet<K>) filter);
    }

    ImMap<K, V> filterFnValues(FunctionSet<V> filter);
    default ImMap<K, V> filterFnValues(SFunctionSet<V> filter) {
        return filterFnValues((FunctionSet<V>) filter);
    }

    ImMap<K, V> filterFn(BiFunction<K, V, Boolean> filter);
    
    ImMap<K, V> splitKeys(BiFunction<K, V, Boolean> keys, Result<ImMap<K, V>> rest);
    
    ImMap<K, V> splitKeys(FunctionSet<K> keys, Result<ImMap<K, V>> rest);
    default ImMap<K, V> splitKeys(SFunctionSet<K> keys, Result<ImMap<K, V>> rest) {
        return splitKeys((FunctionSet<K>) keys, rest); 
    }
    

    <EK extends K> ImMap<EK, V> filter(ImSet<? extends EK> keys);
    <EK extends K> ImMap<EK, V> filterIncl(ImSet<? extends EK> keys);
    <EV extends V> ImMap<K, EV> filterValues(ImSet<EV> values);
    ImMap<K, V> remove(ImSet<? extends K> keys);
    ImMap<K, V> remove(K key);
    ImMap<K, V> removeIncl(K key);
    ImMap<K, V> removeIncl(ImSet<? extends K> keys);
    ImMap<K, V> removeValues(V value); // желательно не менять если нет
    ImMap<K, V> removeNulls();
    ImMap<K, V> removeEquals(ImMap<K, V> map);

    // replaces

    ImMap<K, V> replaceValues(V value);
    ImMap<K, V> override(K key, V value);
    ImMap<K, V> replaceValue(K key, V value);
    ImMap<K, V> replaceValues(ImMap<? extends V, ? extends V> map);
    ImMap<K,V> override(ImMap<? extends K,? extends V> map); // перекрываем this, значениями из map, replace в BaseUtils !!! тут важно разделить те которые добавляют и нет
    ImMap<K,V> overrideIncl(ImMap<? extends K,? extends V> map);

    <M> ImValueMap<K,M> mapItValues();
    <M> ImRevValueMap<K,M> mapItRevValues();

    <M> ImMap<K,M> mapItValues(Function<V, M> getter); // with aftereffect
    <M> ImMap<K,M> mapItValues(BiFunction<K, V, M> getter); // with aftereffect
    <E1 extends Exception, E2 extends Exception> ImMap<K,V> mapItIdentityValuesEx(ThrowingFunction<V, V, E1,E2> getter) throws E1, E2; // with aftereffect, identity optimization
    void iterate(BiConsumer<K, V> consumer);

    // "функциональщина"
    <M> ImMap<K,M> mapValues(Function<V, M> getter);
    <M> ImMap<K,M> mapKeyValues(Function<K, M> getter);
    <M> ImMap<K,M> mapValues(Supplier<M> getter);
    <M> ImMap<K,M> mapValues(BiFunction<K, V, M> getter);
    <M> ImRevMap<K,M> mapRevValues(IntFunction<M> getter);
    <M> ImRevMap<K,M> mapRevValues(Function<V, M> getter);
    <M> ImRevMap<K,M> mapRevValues(BiFunction<K, V, M> getter);
    <MK, MV> ImMap<MK,MV> mapKeyValues(Function<K, MK> getterKey, Function<V, MV> getterValue);
    <MK, MV> ImMap<MK,MV> mapKeyValues(Function<K, MK> getterKey, BiFunction<K, V, MV> getterValue);
    <MK, MV> ImMap<MK,MV> mapKeyValues(BiFunction<K, V, MK> getterKey, BiFunction<K, V, MV> getterValue);

    <M, E1 extends Exception, E2 extends Exception> ImMap<K,M> mapKeyValuesEx(ThrowingFunction<K, M, E1,E2> getter) throws E1, E2;
    <M, E1 extends Exception, E2 extends Exception> ImMap<K,M> mapValuesEx(ThrowingFunction<V, M, E1,E2> getter) throws E1, E2;

    <M> ImSet<M> mapMergeSetValues(BiFunction<K, V, M> getter);
    <M> ImSet<M> mapSetValues(BiFunction<K, V, M> getter);

    <M> ImMap<M,V> mapKeys(Function<K, M> getter);

    <M> ImCol<M> mapColValues(BiFunction<K, V, M> getter);

    ImMap<K, V> mapAddValues(ImMap<K, V> map, AddValue<K, V> addValue);

    String toString(String conc, String delimiter);
    String toString(BiFunction<K, V, String> getter, String delimiter);

    ImOrderMap<K, V> sort(Comparator<K> comparator);
    ImOrderMap<K, V> sort();

    Map<K, V> toJavaMap();
    
    Function<K, V> fnGetValue();
}

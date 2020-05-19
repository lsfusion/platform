package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface ImSet<T> extends FunctionSet<T>, ImCol<T> {

    boolean intersect(ImSet<? extends T> set);
    boolean intersectFn(FunctionSet<T> set);
    boolean disjoint(ImSet<? extends T> col);
    boolean containsAll(ImSet<? extends T> wheres);

    <G> ImMap<G, ImSet<T>> group(BaseUtils.Group<G, T> getter);

    <V> ImCol<V> map(ImMap<T, ? extends V> map);
    <EV> ImSet<EV> mapRev(ImRevMap<T, EV> map);

    ImSet<T> merge(ImSet<? extends T> merge);
    ImSet<T> merge(T element);
    ImSet<T> addExcl(ImSet<? extends T> merge);
    ImSet<T> addExcl(T element);

    // фильтрация

    <M> ImFilterValueMap<T, M> mapFilterValues();
    <M> ImFilterRevValueMap<T, M> mapFilterRevValues();

    ImSet<T> filterFn(FunctionSet<T> filter);
    <E1 extends Exception, E2 extends Exception> ImSet<T> filterFnEx(ThrowingPredicate<T, E1, E2> filter) throws E1, E2;
    default ImSet<T> filterFn(SFunctionSet<T> filter) {
        return filterFn((FunctionSet<T>) filter);    
    }

    ImSet<T> split(FunctionSet<T> filter, Result<ImSet<T>> rest);
    default ImSet<T> split(SFunctionSet<T> filter, Result<ImSet<T>> rest) {
        return split((FunctionSet<T>) filter, rest);
    }
    
    ImSet<T> split(ImSet<T> filter, Result<ImSet<T>> rest, Result<ImSet<T>> restSplit);

    ImSet<T> filter(ImSet<? extends T> filter);
    ImSet<T> remove(ImSet<? extends T> remove);
    ImSet<T> removeIncl(ImSet<? extends T> remove);
    ImSet<T> removeIncl(T element);

    T getIdentIncl(T element);

    <V> ImMap<T, V> toMap(V value);
    ImMap<T, T> toMap();
    ImRevMap<T, T> toRevMap();
    ImOrderSet<T> toOrderSet();
    ImOrderSet<T> sort(); // natural ordering
    ImOrderSet<T> sortSet(Comparator<T> comparator);

    <M> ImValueMap<T, M> mapItValues();
    <M> ImRevValueMap<T, M> mapItRevValues();

    <M> ImMap<T,M> mapItValues(Function<T, M> getter); // с последействием
    <M> ImSet<M> mapItSetValues(Function<T, M> getter); // с последействием

    <M> ImSet<M> mapSetValues(Function<T, M> getter);

    <M> ImMap<T, M> mapValues(Supplier<M> getter);
    <M> ImMap<T, M> mapValues(IntFunction<M> getter);
    <M> ImMap<T, M> mapValues(Function<T, M> getter);
    <MK, MV> ImMap<MK,MV> mapKeyValues(Function<T, MK> getterKey, Function<T, MV> getterValue);
    <MK, MV> ImRevMap<MK,MV> mapRevKeyValues(Function<T, MK> getterKey, Function<T, MV> getterValue);
    <M> ImRevMap<T, M> mapRevValues(IntFunction<M> getter);
    <M> ImRevMap<T, M> mapRevValues(IntObjectFunction<T, M> getter);
    <M> ImRevMap<T, M> mapRevValues(Supplier<M> getter);
    <M> ImRevMap<T, M> mapRevValues(Function<T, M> getter);
    <M> ImRevMap<M, T> mapRevKeys(Supplier<M> getter);
    <M> ImRevMap<M, T> mapRevKeys(Function<T, M> getter);
    <M> ImRevMap<M, T> mapRevKeys(IntFunction<M> getter);

    <M, E1 extends Exception, E2 extends Exception> ImMap<T,M> mapValuesEx(ThrowingFunction<T, M, E1,E2> getter) throws E1, E2;

    Set<T> toJavaSet();
}

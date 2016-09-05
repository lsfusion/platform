package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.FunctionSet;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.*;

public interface ImSet<T> extends FunctionSet<T>, ImCol<T> {

    boolean intersect(ImSet<? extends T> set);
    boolean intersect(FunctionSet<? extends T> set);
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

    ImSet<T> filterFn(FunctionSet<T> filter);
    ImSet<T> split(FunctionSet<T> filter, Result<ImSet<T>> rest);
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

    <M> ImValueMap<T, M> mapItValues();
    <M> ImRevValueMap<T, M> mapItRevValues();

    <M> ImMap<T,M> mapItValues(GetValue<M,T> getter); // с последействием
    <M> ImSet<M> mapItSetValues(GetValue<M, T> getter); // с последействием

    <M> ImSet<M> mapSetValues(GetValue<M, T> getter);

    <M> ImMap<T, M> mapValues(GetStaticValue<M> getter);
    <M> ImMap<T, M> mapValues(GetIndex<M> getter);
    <M> ImMap<T, M> mapValues(GetValue<M, T> getter);
    <MK, MV> ImMap<MK,MV> mapKeyValues(GetValue<MK, T> getterKey, GetValue<MV, T> getterValue);
    <MK, MV> ImRevMap<MK,MV> mapRevKeyValues(GetValue<MK, T> getterKey, GetValue<MV, T> getterValue);
    <M> ImRevMap<T, M> mapRevValues(GetIndex<M> getter);
    <M> ImRevMap<T, M> mapRevValues(GetIndexValue<M, T> getter);
    <M> ImRevMap<T, M> mapRevValues(GetStaticValue<M> getter);
    <M> ImRevMap<T, M> mapRevValues(GetValue<M, T> getter);
    <M> ImRevMap<M, T> mapRevKeys(GetStaticValue<M> getter);
    <M> ImRevMap<M, T> mapRevKeys(GetValue<M,T> getter);
    <M> ImRevMap<M, T> mapRevKeys(GetIndex<M> getter);

    Set<T> toJavaSet();
}

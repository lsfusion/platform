package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.mutable.mapvalue.IntObjectFunction;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

// can contain nulls
public interface ImList<K> extends Iterable<K> {

    int size();
    K get(int i);
    boolean isEmpty();
    K single();

    <G> ImMap<G, ImList<K>> groupList(BaseUtils.Group<G, K> getter);
    
    K[] toArray(K[] array);
    
    ImCol<K> getCol();

    int indexOf(K key);
    boolean containsNull();
    ImMap<Integer, K> toIndexedMap();

    ImList<K> addList(ImList<? extends K> list);
    ImList<K> addList(K element);

    ImList<K> reverseList();
    ImList<K> subList(int i, int to);
    ImList<K> remove(int i);

    <V> ImList<V> mapList(ImMap<? extends K, ? extends V> imMap);

    ImOrderSet<K> toOrderSet();
    ImOrderSet<K> toOrderExclSet();

    // фильтрация
    ImList<K> filterList(FunctionSet<K> filter);
    default ImList<K> filterList(SFunctionSet<K> filter) {
        return filterList((FunctionSet<K>) filter);
    } 

    <M> ImList<M> mapItListValues(Function<K, M> getter); // с последействием

    <M> ImList<M> mapListValues(IntObjectFunction<K, M> getter);
    <M> ImList<M> mapListValues(Function<K, M> getter);

    <M> ImMap<M,K> mapListMapValues(IntFunction<M> getterKey);
    <MK, MV> ImMap<MK,MV> mapListKeyValues(IntFunction<MK> getterKey, Function<K, MV> getterValue);
    <MK, MV> ImMap<MK,MV> mapListKeyValues(Function<K, MK> getterKey, Function<K, MV> getterValue);
    <MK, MV> ImRevMap<MK,MV> mapListRevKeyValues(IntFunction<MK> getterKey, Function<K, MV> getterValue);

    String toString(String separator);
    String toString(Function<K, String> getter, String delimiter);
    String toString(Supplier<String> getter, String delimiter);
    String toString(IntObjectFunction<K, String> getter, String delimiter);

    List<K> toJavaList();

    K last();
}

package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.col.interfaces.mutable.mapvalue.IntObjectFunction;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface ImCol<T> extends Iterable<T> {

    int size();
    T get(int i);
    boolean isEmpty();
    T single();

    ImSet<T> toSet();
    ImList<T> toList();

    ImCol<T> mergeCol(ImCol<T> col);
    ImCol<T> addCol(T element);

    ImCol<T> filterCol(FunctionSet<T> filter);
    default ImCol<T> filterCol(SFunctionSet<T> filter) {
        return filterCol((FunctionSet<T>) filter);
    }
    
    ImMap<T, Integer> multiSet();

    <M> ImCol<M> mapColValues(IntObjectFunction<T, M> getter);
    <M> ImCol<M> mapColValues(Function<T, M> getter);
    <M> ImSet<M> mapColSetValues(IntObjectFunction<T, M> getter);
    <M> ImSet<M> mapColSetValues(Function<T, M> getter);
    <M> ImSet<M> mapMergeSetValues(Function<T, M> getter);
    <M> ImSet<M> mapMergeSetSetValues(Function<T, ImSet<M>> getter);

    <M> ImMap<M, T> mapColKeys(IntFunction<M> getter);

    String toString(String separator);
    String toString(Function<T, String> getter, String delimiter);
    String toString(Supplier<String> getter, String delimiter);

    ImList<T> sort(Comparator<T> comparator);
    
    Collection<T> toJavaCol();
    
    T[] toArray(T[] array);
}

package lsfusion.base.col.interfaces.immutable;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.*;

import java.util.Collection;
import java.util.Comparator;

public interface ImCol<T> extends Iterable<T> {

    int size();
    T get(int i);
    boolean isEmpty();
    T single();

    ImSet<T> toSet();
    ImList<T> toList();

    ImCol<T> mergeCol(ImCol<T> col);
    
    ImCol<T> filterCol(FunctionSet<T> filter);
    
    ImMap<T, Integer> multiSet();

    <M> ImCol<M> mapColValues(GetIndexValue<M, T> getter);
    <M> ImCol<M> mapColValues(GetValue<M, T> getter);
    <M> ImSet<M> mapColSetValues(GetIndexValue<M, T> getter);
    <M> ImSet<M> mapColSetValues(GetValue<M, T> getter);
    <M> ImSet<M> mapMergeSetValues(GetValue<M, T> getter);

    <M> ImMap<M, T> mapColKeys(GetIndex<M> getter);

    String toString(String separator);
    String toString(GetValue<String, T> getter, String delimiter);
    String toString(GetStaticValue<String> getter, String delimiter);

    ImList<T> sort(Comparator<T> comparator);
    
    Collection<T> toJavaCol();
}

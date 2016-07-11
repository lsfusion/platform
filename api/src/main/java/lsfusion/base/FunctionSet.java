package lsfusion.base;

import lsfusion.base.col.interfaces.mutable.SimpleAddValue;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;

public interface FunctionSet<T> {

    SimpleAddValue<Object, FunctionSet<Object>> mergeSet = new SymmAddValue<Object, FunctionSet<Object>>() {
        public FunctionSet<Object> addValue(Object key, FunctionSet<Object> prevValue, FunctionSet<Object> newValue) {
            return BaseUtils.merge(prevValue, newValue);
        }
    };

    boolean contains(T element);

    boolean isEmpty();

    boolean isFull();
}

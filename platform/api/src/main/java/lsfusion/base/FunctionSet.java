package lsfusion.base;

import lsfusion.base.col.interfaces.mutable.SimpleAddValue;

public interface FunctionSet<T> {

    public final static SimpleAddValue<Object, FunctionSet<Object>> mergeSet = new SimpleAddValue<Object, FunctionSet<Object>>() {
        public FunctionSet<Object> addValue(Object key, FunctionSet<Object> prevValue, FunctionSet<Object> newValue) {
            return BaseUtils.merge(prevValue, newValue);
        }

        public boolean symmetric() {
            return true;
        }
    };

    boolean contains(T element);

    boolean isEmpty();

    boolean isFull();
}

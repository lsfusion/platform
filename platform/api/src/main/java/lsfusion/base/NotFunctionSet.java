package lsfusion.base;

public class NotFunctionSet<K> implements FunctionSet<K> {
    private final FunctionSet<K> set;

    public NotFunctionSet(FunctionSet<K> set) {
        this.set = set;
    }

    public boolean contains(K element) {
        return !set.contains(element);
    }

    public boolean isEmpty() {
        return set.isFull();
    }

    @Override
    public boolean isFull() {
        return set.isEmpty();
    }
}

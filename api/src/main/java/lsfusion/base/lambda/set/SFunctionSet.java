package lsfusion.base.lambda.set;

@FunctionalInterface
public interface SFunctionSet<K> extends FunctionSet<K> {
    @Override
    default boolean isEmpty() {
        return false;
    }

    @Override
    default boolean isFull() {
        return false;
    }
}

package lsfusion.base.lambda.set;

public interface FunctionSet<T> {

    boolean contains(T element);

    boolean isEmpty();

    boolean isFull();
}

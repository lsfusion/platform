package platform.base;

public interface FunctionSet<T> {
    
    boolean contains(T element);

    boolean isEmpty();

    boolean isFull();
}

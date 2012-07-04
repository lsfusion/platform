package platform.base;

public class EmptyFunctionSet<T> implements FunctionSet<T> {
    
    private EmptyFunctionSet() {
    }
    
    private final static EmptyFunctionSet instance = new EmptyFunctionSet();

    public static <T> EmptyFunctionSet<T> instance() {
        return instance;
    }

    public boolean contains(T element) {
        return false;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean isFull() {
        return false;
    }
}

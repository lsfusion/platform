package platform.base;

// equals и hashCode нет смысла перегружать так как singleton
public class FullFunctionSet<T> implements FunctionSet<T> {

    private FullFunctionSet() {
    }

    private final static FullFunctionSet instance = new FullFunctionSet();

    public static <T> FullFunctionSet<T> instance() {
        return instance;
    }

    public boolean contains(T element) {
        return true;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isFull() {
        return true;
    }
}

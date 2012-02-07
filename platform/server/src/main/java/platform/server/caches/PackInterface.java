package platform.server.caches;

public interface PackInterface<T> {

    T pack();
    long getComplexity(boolean outer);

}

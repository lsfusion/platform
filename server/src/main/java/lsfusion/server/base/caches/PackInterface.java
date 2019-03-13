package lsfusion.server.base.caches;

public interface PackInterface<T> {

    T pack();
    long getComplexity(boolean outer);

}

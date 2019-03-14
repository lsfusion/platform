package lsfusion.server.data.pack;

public interface PackInterface<T> {

    T pack();
    long getComplexity(boolean outer);

}

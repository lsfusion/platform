package lsfusion.base.lambda;

public interface EConsumer<T, E extends Exception> {

    void accept(T param) throws E;
}

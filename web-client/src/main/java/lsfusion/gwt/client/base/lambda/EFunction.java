package lsfusion.gwt.client.base.lambda;

public interface EFunction<T, R, E extends Exception> {

    R apply(T param) throws E;
}

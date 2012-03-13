package platform.server.form.instance.remote;

public interface InvocationHandler<T, E extends Exception> {
    T handle(InvocationResult invocationResult) throws E;
}

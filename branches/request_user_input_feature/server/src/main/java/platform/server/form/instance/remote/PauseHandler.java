package platform.server.form.instance.remote;

public interface PauseHandler<T, E extends Exception> {
    T handle(InvocationResult invocationResult) throws E;
}

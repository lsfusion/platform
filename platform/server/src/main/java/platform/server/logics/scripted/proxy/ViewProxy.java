package platform.server.logics.scripted.proxy;

public abstract class ViewProxy<T> {
    protected final T target;

    public ViewProxy(T target) {
        this.target = target;
    }
}

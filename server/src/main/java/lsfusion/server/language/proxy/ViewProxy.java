package lsfusion.server.language.proxy;

public abstract class ViewProxy<T> {
    protected final T target;

    public ViewProxy(T target) {
        this.target = target;
    }
}

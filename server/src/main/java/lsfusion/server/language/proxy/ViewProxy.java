package lsfusion.server.language.proxy;

import lsfusion.server.base.version.Version;

public abstract class ViewProxy<T> {
    protected final T target;

    public ViewProxy(T target) {
        this.target = target;
    }

    protected static Version getVersion() {
        return ViewProxyUtil.getVersion();
    }
}

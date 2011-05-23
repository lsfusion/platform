package platform.server.caches;

import platform.base.ImmutableObject;
import platform.base.TwinImmutableObject;
import platform.server.caches.hash.HashContext;

public abstract class AbstractOuterContext<This extends OuterContext> extends TwinImmutableObject implements OuterContext<This> {

    public int immutableHashCode() {
        return hashOuter(HashContext.hashCode);
    }
}

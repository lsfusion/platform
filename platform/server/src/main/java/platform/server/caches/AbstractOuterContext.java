package platform.server.caches;

import platform.base.ImmutableObject;
import platform.server.caches.hash.HashContext;

public abstract class AbstractOuterContext<This extends OuterContext> extends ImmutableObject implements OuterContext<This> {

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashOuter(HashContext.hashCode);
            hashCoded = true;
        }
        return hashCode;
    }
}

package platform.server.caches;

import platform.server.caches.hash.HashContext;
import platform.base.ImmutableObject;

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

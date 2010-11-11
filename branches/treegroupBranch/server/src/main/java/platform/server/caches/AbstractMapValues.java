package platform.server.caches;

import platform.base.ImmutableObject;
import platform.server.caches.hash.HashCodeValues;

public abstract class AbstractMapValues<U extends AbstractMapValues<U>> extends ImmutableObject implements MapValues<U>  {

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashValues(HashCodeValues.instance);
            hashCoded = true;
        }
        return hashCode;
    }
}

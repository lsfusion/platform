package platform.server.caches.hash;

import platform.base.ImmutableObject;
import platform.server.data.Value;

public abstract class HashValues extends HashObject {

    public abstract int hash(Value expr);
}

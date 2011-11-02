package platform.server.caches.hash;

import platform.base.ImmutableObject;
import platform.server.data.Value;

import java.util.Set;

public abstract class HashValues extends HashObject {

    public abstract int hash(Value expr);

    public abstract HashValues filterValues(Set<Value> values);
}

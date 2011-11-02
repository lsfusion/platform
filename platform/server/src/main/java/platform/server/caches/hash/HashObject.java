package platform.server.caches.hash;

import platform.base.ImmutableObject;
import platform.server.data.Value;

import java.security.Identity;
import java.util.IdentityHashMap;
import java.util.Set;

public abstract class HashObject {

    public abstract HashObject filterValues(Set<Value> values);
}

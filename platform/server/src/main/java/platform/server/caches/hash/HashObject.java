package platform.server.caches.hash;

import platform.base.ImmutableObject;

import java.security.Identity;
import java.util.IdentityHashMap;

public abstract class HashObject {

    private IdentityHashMap<Object, Integer> identityCaches;
    public IdentityHashMap<Object, Integer> getIdentityCaches() {
        if(identityCaches==null)
            identityCaches = new IdentityHashMap<Object, Integer>();
        return identityCaches;
    }

    public abstract boolean isGlobal();
}

package platform.server.caches.hash;

import platform.server.caches.AbstractHashContext;

import java.util.IdentityHashMap;

public abstract class HashObject {

    public abstract boolean isGlobal();

    private IdentityHashMap<AbstractHashContext, Integer> caches;
    public Integer aspectGetCache(AbstractHashContext context) {
        if(caches==null)
             caches = new IdentityHashMap<AbstractHashContext, Integer>();
        return caches.get(context);
    }
    public void aspectSetCache(AbstractHashContext context, Integer result) {
        if(caches==null)
             caches = new IdentityHashMap<AbstractHashContext, Integer>();
        caches.put(context, result);
    }

}

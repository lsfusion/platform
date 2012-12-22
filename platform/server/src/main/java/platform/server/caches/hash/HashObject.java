package platform.server.caches.hash;

import platform.server.caches.AbstractTranslateContext;

import java.util.IdentityHashMap;

public abstract class HashObject {

    public abstract boolean isGlobal();

    private IdentityHashMap<AbstractTranslateContext, Integer> caches;
    public Integer aspectGetCache(AbstractTranslateContext context) {
        if(caches==null)
             caches = new IdentityHashMap<AbstractTranslateContext, Integer>();
        return caches.get(context);
    }
    public void aspectSetCache(AbstractTranslateContext context, Integer result) {
        if(caches==null)
             caches = new IdentityHashMap<AbstractTranslateContext, Integer>();
        caches.put(context, result);
    }

}

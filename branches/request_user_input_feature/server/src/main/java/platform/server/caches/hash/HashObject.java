package platform.server.caches.hash;

import platform.base.ImmutableObject;
import platform.base.QuickMap;
import platform.base.SimpleMap;
import platform.server.caches.AbstractTranslateContext;
import platform.server.data.Value;
import platform.server.data.translator.MapTranslate;

import java.security.Identity;
import java.util.IdentityHashMap;
import java.util.Set;

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

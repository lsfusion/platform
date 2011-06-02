package platform.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

public abstract class ImmutableObject {
    
    private Map caches = null;
    public Map getCaches() {
        if(caches==null) caches = new HashMap();
        return caches;
    }

}

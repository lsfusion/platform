package platform.base;

import java.util.Map;
import java.util.HashMap;

public class ImmutableObject {
    
    private Map caches = null;
    public Map getCaches() {
        if(caches==null) caches = new HashMap();
        return caches;
    }

}

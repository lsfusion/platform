package lsfusion.server.logics.mutables;

import lsfusion.server.classes.CustomClass;

import java.util.HashMap;
import java.util.Map;

public abstract class SIDHandler<K> {

    private final Map<String, K> sidToObject = new HashMap<String, K>();
    
    protected abstract String getSID(K object);

    @NFLazy
    public void store(K object) {
        String sid = getSID(object);
        assert !checkUnique() || !sidToObject.containsKey(sid);
        sidToObject.put(sid, object);
    }
    
    public boolean checkUnique() {
        return true;
    }

    @NFLazy
    public K find(String sid) {
        return sidToObject.get(sid);
    }
    
    @NFLazy
    public void remove(K object) {
        sidToObject.remove(getSID(object));
    }

}

package lsfusion.server.logics.mutables;

import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.mutables.interfaces.NFMapList;

import java.util.Collections;
import java.util.Iterator;

public abstract class SIDHandler<K> {

    private final NFMapList<String, K> sidToObject = NFFact.mapList();
    
    protected abstract String getSID(K object);

    public void store(K object, Version version) {
        String sid = getSID(object);
        NFList<K> nfList = sidToObject.getNFList(sid);
        assert !checkUnique() || nfList == null || nfList.getNFList(version).isEmpty();
        sidToObject.addAll(sid, Collections.singletonList(object), version);
    }
    
    public boolean checkUnique() {
        return true;
    }

    public K find(String sid, Version version) {
        NFList<K> nfList = sidToObject.getNFList(sid);
        if (nfList == null) {
            return null;
        } else {
            Iterator<K> iterator = nfList.getNFListIt(version).iterator();
            return iterator.hasNext() ? iterator.next() : null;
        }
    }
    
    public void remove(K object, Version version) {
        sidToObject.removeAll(getSID(object), version);
    }

    public void finalizeChanges() {
        sidToObject.finalizeChanges();
    }
}

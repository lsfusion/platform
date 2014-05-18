    package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFDefault;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.mutables.interfaces.NFProperty;

public class NFPropertyImpl<K> extends NFImpl<NFList<K>, K> implements NFProperty<K> {

    public NFPropertyImpl() {
    }

    public NFPropertyImpl(boolean allowVersionFinalRead) {
        super(allowVersionFinalRead);
    }

    public NFPropertyImpl(K changes) {
        super(changes);
    }

    protected NFList<K> initMutable() {
        return new NFListImpl<K>();
    }

    public K getNF(Version version) {
        K result = proceedFinal(version);
        if(result!=null)
            return result;
        
        ImList<K> list = getChanges().getNFList(version);
        int last = list.size();
        if(last == 0)
            return null;
        else
            return list.get(list.size() - 1);
    }

    protected boolean checkFinal(Object object) {
        return !(object instanceof NFList);
    }

    public void set(K value, Version version) {
        getChanges().add(value, version);
    }

    public K get() {
        return getFinal();
    }

    public K getDefault(NFDefault<K> def) {
        K value = get();
        if(value != null)
            return value;
        
        synchronized (this) {
            value = get();
            if(value != null)
                return value;
            
            value = def.create();
            setFinal(value);
            return value;
        }
    }
}

package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFDefault;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.mutables.interfaces.NFProperty;

import java.lang.ref.WeakReference;

public class NFPropertyImpl<K> extends NFImpl<NFList<K>, K> implements NFProperty<K> {

    public NFPropertyImpl() {
    }

    private WeakReference<Object> debugInfo;
    protected String getDebugInfo() {
        Object obj;
        if(debugInfo != null && (obj = debugInfo.get()) != null)
            return obj.toString();
        return super.getDebugInfo();
    }

    public NFPropertyImpl(boolean allowVersionFinalRead, Object debugInfo) {
        super(allowVersionFinalRead);
        if(debugInfo != null)
            this.debugInfo = new WeakReference<Object>(debugInfo);        
    }

    public NFPropertyImpl(K changes) {
        super(changes);
    }

    protected NFList<K> initMutable() {
        return new NFListImpl<K>();
    }

    public K getNF(Version version) {
        if(checkVersionFinal(version)) // не proceedVersionFinal, так как результат может быть null и его не отличишь
            return getFinalChanges();
        
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

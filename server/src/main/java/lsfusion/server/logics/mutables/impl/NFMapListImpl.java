package lsfusion.server.logics.mutables.impl;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.server.logics.mutables.NFFact;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.mutables.interfaces.NFMapList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NFMapListImpl<K, V> extends NFImpl<Map<K, NFList<V>>, ImMap<K, ImList<V>>> implements NFMapList<K, V> {

    protected Map<K, NFList<V>> initMutable() {
        return new ConcurrentHashMap<K, NFList<V>>();
    }

    public NFMapListImpl() {
    }

    public NFMapListImpl(ImMap<K, ImList<V>> changes) {
        super(changes);
    }

    protected boolean checkFinal(Object object) {
        return object instanceof ImMap;
    }

    public ImMap<K, ImList<V>> getNF(Version version) {
        Map<K, NFList<V>> changes = getChanges();
        MExclMap<K, ImList<V>> mResult = MapFact.mExclMap();
        for(Map.Entry<K, NFList<V>> entry : changes.entrySet())
            mResult.exclAdd(entry.getKey(), entry.getValue().getNFList(version));
        return mResult.immutable();
    }
    
    public NFList<V> getNFList(K key) {
        return getChanges().get(key);
    }

    public ImMap<K, ImList<V>> getOrderMap() {
        return getFinal();
    }

    public Iterable<V> getListIt(K key) {
        ImList<V> list = getOrderMap().get(key);
        if(list == null)
            list = ListFact.EMPTY();
        return list;
    }

    public void addAll(K key, Iterable<V> it, Version version) {
        Map<K, NFList<V>> map = getChanges();
        NFList<V> nfList = map.get(key);
        if(nfList == null) {
            synchronized (this) {
                nfList = map.get(key);
                if(nfList == null) {
                    nfList = NFFact.list();
                    map.put(key, nfList);
                }
            }
        }
        
        for(V value : it)
            nfList.add(value, version);
    }

    public void removeAll(K key, Version version) {
        NFList<V> nfList = getChanges().get(key);
        if(nfList!=null)
            nfList.removeAll(version);
    }
}

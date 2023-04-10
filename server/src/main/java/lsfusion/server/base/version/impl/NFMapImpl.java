package lsfusion.server.base.version.impl;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFMapAdd;
import lsfusion.server.base.version.impl.changes.NFMapChange;
import lsfusion.server.base.version.interfaces.NFMap;

public class NFMapImpl<K, V> extends NFChangeImpl<NFMapChange<K, V>, ImMap<K, V>> implements NFMap<K, V> {

    public NFMapImpl() {
    }

    public ImMap<K, V> getNF(Version version) {
        ImMap<K, V> result = proceedVersionFinal(version);
        if(result!=null)
            return result;

        final MMap<K, V> mMap = MapFact.mMap(MapFact.override());
        proceedChanges(change -> change.proceedMap(mMap), version);

        return mMap.immutable();
    }

    public ImMap<K, V> getMap() {
        return getFinal();
    }

    public V getNFValue(K key, Version version) {
        return getNF(version).get(key);
    }

    public void add(K key, V value, Version version) {
        addChange(new NFMapAdd<>(key, value), version);
    }

    @Override
    protected boolean checkFinal(Object object) {
        return object instanceof ImMap;
    }
}

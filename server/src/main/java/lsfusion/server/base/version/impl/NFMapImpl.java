package lsfusion.server.base.version.impl;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFMapAdd;
import lsfusion.server.base.version.impl.changes.NFMapChange;
import lsfusion.server.base.version.impl.changes.NFMapCopy;
import lsfusion.server.base.version.interfaces.NFMap;

import java.util.function.Function;

public class NFMapImpl<K, V> extends NFChangeImpl<NFMapChange<K, V>, ImMap<K, V>> implements NFMap<K, V> {

    public NFMapImpl() {
    }

    public ImMap<K, V> getNF(Version version) {
        return getNF(version, false);
    }

    @Override
    public ImMap<K, V> getNFCopy(Version version) {
        return getNF(version, true);
    }

    public ImMap<K, V> getNF(Version version, boolean allowRead) {
        ImMap<K, V> result = proceedVersionFinal(version, allowRead);
        if(result!=null)
            return result;

        final MMap<K, V> mMap = MapFact.mMap(MapFact.override());
        proceedChanges((change, nextChange) -> change.proceedMap(mMap, version), version);

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
    public void add(NFMap<K, V> map, Function<V, V> mapping, Version version) {
        addChange(new NFMapCopy<>(map, mapping), version);
    }

    @Override
    protected boolean checkFinal(Object object) {
        return object instanceof ImMap;
    }
}

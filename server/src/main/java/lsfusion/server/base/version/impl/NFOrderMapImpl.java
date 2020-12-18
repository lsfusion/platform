package lsfusion.server.base.version.impl;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.base.version.FindIndex;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.changes.NFMapAdd;
import lsfusion.server.base.version.impl.changes.NFMapMove;
import lsfusion.server.base.version.impl.changes.NFOrderMapChange;
import lsfusion.server.base.version.interfaces.NFOrderMap;

import java.util.List;

public class NFOrderMapImpl<K, V> extends NFChangeImpl<NFOrderMapChange<K, V>, ImOrderMap<K, V>> implements NFOrderMap<K, V> {

    public NFOrderMapImpl() {
    }

    public NFOrderMapImpl(ImOrderMap<K, V> changes) {
        super(changes);
    }

    public ImOrderMap<K, V> getNF(Version version) {
        ImOrderMap<K, V> result = proceedVersionFinal(version);
        if(result!=null)
            return result;

        final List<K> mKeys = SetFact.mAddRemoveOrderSet();
        final List<V> mValues = SetFact.mAddRemoveOrderSet();
        proceedChanges(change -> change.proceedOrderMap(mKeys, mValues), version);
        OrderedMap<K, V> mMap = MapFact.mAddRemoveOrderMap();

        for(int i = 0; i < mKeys.size(); i++) {
            mMap.put(mKeys.get(i), mValues.get(i));
        }

        return MapFact.fromJavaOrderMap(mMap);
    }

    public ImOrderMap<K, V> getListMap() {
        return getFinal();
    }

    public V getNFValue(K key, Version version) {
        return getNF(version).get(key);
    }

    private static final FindIndex moveFirst = list -> 0;

    @Override
    public void addFirst(K key, V value, Version version) {
        add(key, value, moveFirst, version);
    }

    @Override
    public void add(K key, V value, FindIndex<K> finder, Version version) {
        add(key, value, version);
        move(key, value, finder, version);
    }

    public void add(K key, V value, Version version) {
        addChange(new NFMapAdd<>(key, value), version);
    }

    public void move(K key, V value, FindIndex<K> finder, Version version) {
        addChange(new NFMapMove<>(key, value, finder), version);
    }

    @Override
    protected boolean checkFinal(Object object) {
        return object instanceof ImOrderMap;
    }
}

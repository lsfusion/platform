package lsfusion.server.logics.mutables.impl;

import lsfusion.base.Pair;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.impl.changes.NFAdd;
import lsfusion.server.logics.mutables.interfaces.NFOrderMap;

public class NFOrderMapImpl<K, V> extends NFChangeImpl<Pair<K, V>, NFAdd<Pair<K, V>>, ImOrderMap<K, V>> implements NFOrderMap<K, V> {

    public NFOrderMapImpl() {
    }

    public NFOrderMapImpl(ImOrderMap<K, V> changes) {
        super(changes);
    }

    public ImOrderMap<K, V> getNF(Version version) {
        final MOrderMap<K, V> mOrderMap = MapFact.mOrderMap(MapFact.<K, V>override());
        proceedChanges(new ChangeProcessor<Pair<K, V>, NFAdd<Pair<K, V>>>() {
            public void proceed(NFAdd<Pair<K, V>> change) {
                Pair<K, V> pair = change.element;
                mOrderMap.add(pair.first, pair.second);
            }
        }, version);
        return mOrderMap.immutableOrder();
    }

    public ImOrderMap<K, V> getListMap() {
        return getFinal();
    }

    public V getNFValue(K key, Version version) {
        return getNF(version).get(key);
    }

    public void add(K key, V value, Version version) {
        addChange(new NFAdd<>(new Pair<>(key, value)), version);
    }

    @Override
    protected boolean checkFinal(Object object) {
        return object instanceof ImOrderMap;
    }
}

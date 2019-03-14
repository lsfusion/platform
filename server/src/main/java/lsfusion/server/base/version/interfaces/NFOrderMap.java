package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

// реализация из OrderSet и MapCol, или из List<Pair>
public interface NFOrderMap<K, V> extends NF {

    void add(K key, V value, Version version);
    
    ImOrderMap<K, V> getListMap();
    V getNFValue(K key, Version version);

}

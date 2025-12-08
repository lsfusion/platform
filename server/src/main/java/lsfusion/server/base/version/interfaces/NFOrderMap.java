package lsfusion.server.base.version.interfaces;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.base.version.FindIndex;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.impl.NF;

import java.util.function.Function;

// реализация из OrderSet и MapCol, или из List<Pair>
public interface NFOrderMap<K, V> extends NF {

    void addFirst(K key, V value, Version version);
    void add(K key, V value, FindIndex<K> finder, Version version);
    void add(K key, V value, Version version);

    void add(NFOrderMap<K, V> map, Function<K, K> mapping, Version version);
    
    ImOrderMap<K, V> getNFCopy(Version version);
    ImOrderMap<K, V> getListMap();
    V getNFValue(K key, Version version);

}

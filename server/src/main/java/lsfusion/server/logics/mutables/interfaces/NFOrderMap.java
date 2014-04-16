package lsfusion.server.logics.mutables.interfaces;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.logics.mutables.Version;
import org.apache.poi.hssf.record.formula.functions.T;

// реализация из OrderSet и MapCol, или из List<Pair>
public interface NFOrderMap<K, V> {

    void add(K key, V value, Version version);
    
    ImOrderMap<K, V> getListMap();
    V getNFValue(K key, Version version);

}

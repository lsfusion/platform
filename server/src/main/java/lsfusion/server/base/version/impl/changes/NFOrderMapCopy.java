package lsfusion.server.base.version.impl.changes;

import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFOrderMap;

import java.util.List;
import java.util.function.Function;

public class NFOrderMapCopy<K, V> implements NFOrderMapChange<K, V> {

    public NFOrderMap<K, V> map;
    public Function<K, K> mapping;
    public NFOrderMapCopy(NFOrderMap<K, V> map, Function<K, K> mapping) {
        this.map = map;
        this.mapping = mapping;
    }

    @Override
    public void proceedOrderMap(List<K> keysList, List<V> valuesList, Version version) {
        ImOrderMap<K, V> nf = map.getNF(version);
        for(int i = 0, size = nf.size(); i < size; i++) {
            keysList.add(mapping.apply(nf.getKey(i)));
            valuesList.add(nf.getValue(i));
        }
    }
}

package platform.base.col.implementations.simple;

import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.mutable.MExclMap;
import platform.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class FilterValueMap<K, V> implements ImFilterValueMap<K, V> {

    private final ImValueMap<K, V> map;

    public FilterValueMap(ImValueMap<K, V> map) {
        this.map = map;
        this.mapped = new int[map.mapSize()];
    }

    private int mapSize = 0;
    private int mapped[];

    public void mapValue(int i, V value) {
        map.mapValue(i, value);
        mapped[mapSize++] = i;
    }

    public ImMap<K, V> immutableValue() {
        if(mapSize<mapped.length) { // пересоздаем
            MExclMap<K,V> mResult = MapFact.mExclMap(mapSize);
            for (int i=0;i<mapSize;i++) {
                int iMap = mapped[i];
                mResult.exclAdd(map.getMapKey(iMap), map.getMapValue(iMap));
            }
            return mResult.immutable();
        } else
            return map.immutableValue();
    }
}

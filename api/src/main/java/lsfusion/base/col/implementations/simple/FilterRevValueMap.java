package lsfusion.base.col.implementations.simple;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MRevMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImRevValueMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

public class FilterRevValueMap<K, V> implements ImFilterRevValueMap<K, V> {

    private final ImRevValueMap<K, V> map;

    public FilterRevValueMap(ImRevValueMap<K, V> map) {
        this.map = map;
        this.mapped = new int[map.mapSize()];
    }

    private int mapSize = 0;
    private int mapped[];

    public void mapValue(int i, V value) {
        map.mapValue(i, value);
        mapped[mapSize++] = i;
    }

    public ImRevMap<K, V> immutableRevValue() {
        if(mapSize<mapped.length) { // пересоздаем
            MRevMap<K,V> mResult = MapFact.mRevMap(mapSize);
            for (int i=0;i<mapSize;i++) {
                int iMap = mapped[i];
                mResult.revAdd(map.getMapKey(iMap), map.getMapValue(iMap));
            }
            return mResult.immutableRev();
        } else
            return map.immutableValueRev();
    }

}

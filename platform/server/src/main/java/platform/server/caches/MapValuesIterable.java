package platform.server.caches;

import platform.base.MapIterable;
import platform.base.QuickMap;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashTranslateValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// нужен чтобы с использованием hash'ей строить мапы ValueExpr'ов
public class MapValuesIterable extends MapIterable<MapValuesTranslate, MapValuesTranslate> {

    private final MapValues<?> from;
    private final MapValues<?> to;

    public MapValuesIterable(MapValues from, MapValues to) {
        this.from = from;
        this.to = to;
    }

    protected MapValuesTranslate map(final MapValuesTranslate map) {
        if(from.hashValues(new HashTranslateValues(map))==to.hashValues(HashCodeValues.instance))
            return map;
        else
            return null;
    }

    protected Iterator<MapValuesTranslate> mapIterator() {
        return new ValuePairs(from.getValues(), to.getValues()).iterator();
    }

    public static <K> int hash(Map<K,? extends MapValues> map, HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<K,? extends MapValues> entry : map.entrySet())
            hash += entry.getKey().hashCode() ^ entry.getValue().hashValues(hashValues);
        return hash;
    }

    public static <K> int hash(QuickMap<K,? extends MapValues> map, HashValues hashValues) {
        int hash = 0;
        for(int i=0;i<map.size;i++)
            hash += map.getKey(i).hashCode() ^ map.getValue(i).hashValues(hashValues);
        return hash;
    }

    public static <K> void enumValues(Set<ValueExpr> values, Map<K,? extends MapValues> map) {
        for(MapValues<?> value : map.values())
            values.addAll(value.getValues());
    }
}

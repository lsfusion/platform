package platform.server.caches;

import platform.base.MapIterable;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashValues;
import platform.server.data.Value;
import platform.server.data.translator.MapValuesTranslate;

import java.util.Iterator;
import java.util.Map;

// нужен чтобы с использованием hash'ей строить мапы ValueExpr'ов
public class MapValuesIterable extends MapIterable<MapValuesTranslate, MapValuesTranslate> {

    private final ValuesContext<?> from;
    private final ValuesContext<?> to;

    public MapValuesIterable(ValuesContext from, ValuesContext to) {
        this.from = from;
        this.to = to;
    }

    protected MapValuesTranslate map(final MapValuesTranslate map) {
        if(from.hashValues(map.getHashValues())==to.hashValues(HashCodeValues.instance))
            return map;
        else
            return null;
    }

    protected Iterator<MapValuesTranslate> mapIterator() {
        return new ValuePairs(from.getValuesMap(), to.getValuesMap()).iterator();
    }

    public static <K> int hash(Map<K,? extends ValuesContext> map, HashValues hashValues) {
        int hash = 0;
        for(Map.Entry<K,? extends ValuesContext> entry : map.entrySet())
            hash += entry.getKey().hashCode() ^ entry.getValue().hashValues(hashValues);
        return hash;
    }

    public static int hash(QuickSet<? extends ValuesContext> set, HashValues hashValues) {
        int hash = 0;
        for(int i=0;i<set.size;i++)
            hash += set.get(i).hashValues(hashValues);
        return hash;
    }
    
    public static <K> int hash(QuickMap<K,? extends ValuesContext> map, HashValues hashValues) {
        int hash = 0;
        for(int i=0;i<map.size;i++)
            hash += map.getKey(i).hashCode() ^ map.getValue(i).hashValues(hashValues);
        return hash;
    }

    public static <K> QuickSet<Value> getContextValues(Map<K, ? extends ValuesContext> map) {
        QuickSet<Value> result = new QuickSet<Value>();
        for(ValuesContext<?> value : map.values())
            result.addAll(value.getContextValues());
        return result;
    }

    public static <K> QuickSet<Value> getContextValues(QuickMap<K, ? extends ValuesContext> map) {
        QuickSet<Value> result = new QuickSet<Value>();
        for(int i=0;i<map.size;i++)
            result.addAll(map.getValue(i).getContextValues());
        return result;
    }

}

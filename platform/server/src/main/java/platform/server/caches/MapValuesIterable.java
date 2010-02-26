package platform.server.caches;

import platform.base.MapIterable;
import platform.base.Pairs;
import platform.base.QuickMap;
import platform.server.data.expr.ValueExpr;

import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;

// нужен чтобы с использованием hash'ей строить мапы ValueExpr'ов
public class MapValuesIterable extends MapIterable<Map<ValueExpr,ValueExpr>,Map<ValueExpr,ValueExpr>> {

    private final MapValues<?> from;
    private final MapValues<?> to;

    public MapValuesIterable(MapValues from, MapValues to) {
        this.from = from;
        this.to = to;
    }

    protected Map<ValueExpr, ValueExpr> map(final Map<ValueExpr, ValueExpr> map) {
        if(from.hashValues(new HashValues(){
            public int hash(ValueExpr expr) {
                return map.get(expr).hashCode();
            }
        })==to.hashValues(new HashValues(){
            public int hash(ValueExpr expr) {
                return expr.hashCode();
            }
        }))
            return map;
        else
            return null;
    }

    protected Iterator<Map<ValueExpr, ValueExpr>> mapIterator() {
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

    public static <K,U extends MapValues<U>> Map<K,U> translate(Map<K,U> map, Map<ValueExpr,ValueExpr> mapValues) {
        Map<K,U> result = new HashMap<K,U>();
        for(Map.Entry<K,U> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().translate(mapValues));
        return result;
    }
}

package lsfusion.server.caches;

import lsfusion.base.MapIterable;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.caches.hash.HashCodeValues;
import lsfusion.server.caches.hash.HashValues;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapValuesTranslate;

import java.util.Iterator;

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

    public static Iterator<MapValuesTranslate> mapIterator(ValuesContext<?> from, ValuesContext<?> to) {
        return new ValuePairs(from.getValueComponents().map, to.getValueComponents().map).iterator();
    }
    protected Iterator<MapValuesTranslate> mapIterator() {
        return mapIterator(from, to);
    }

    public static int hash(ImSet<? extends ValuesContext> set, HashValues hashValues) {
        int hash = 0;
        for(int i=0,size=set.size();i<size;i++)
            hash += set.get(i).hashValues(hashValues);
        return hash;
    }
    
    public static <K> int hash(ImMap<K,? extends ValuesContext> map, HashValues hashValues) {
        int hash = 0;
        for(int i=0,size=map.size();i<size;i++)
            hash += map.getKey(i).hashCode() ^ map.getValue(i).hashValues(hashValues);
        return hash;
    }

    public static <K> ImSet<Value> getContextValues(ImMap<K, ? extends ValuesContext> map) {
        MSet<Value> result = SetFact.mSet();
        for(int i=0,size=map.size();i<size;i++)
            result.addAll(map.getValue(i).getContextValues());
        return result.immutable();
    }

}

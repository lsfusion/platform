package platform.server.caches;

import platform.base.Pairs;
import platform.base.EmptyIterator;
import platform.base.BaseUtils;
import platform.base.MapIterable;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.HashContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;

public class MapParamsIterable implements Iterable<KeyTranslator> {

    public static int hash(MapContext map,final boolean values) {
        return map.hash(new HashContext() {
            public int hash(KeyExpr expr) {
                return 1;
            }
            public int hash(ValueExpr expr) {
                return values?1:expr.hashCode();
            }
        });
    }

    private final MapContext from;
    private final MapContext to;
    private final boolean values;

    public MapParamsIterable(MapContext from, MapContext to, boolean values) {
        this.from = from;
        this.to = to;
        this.values = values;
    }

    public Iterator<KeyTranslator> iterator() {
        return new MapIterator();
    }

    private class ValuePairs extends MapIterable<Map<ValueExpr,ValueExpr>,Map<ValueExpr,ValueExpr>> {
        final Collection<ValueExpr> values1;
        final Collection<ValueExpr> values2;

        private ValuePairs(Collection<ValueExpr> values1, Collection<ValueExpr> values2) {
            this.values1 = values1;
            this.values2 = values2;
        }

        protected Map<ValueExpr, ValueExpr> map(Map<ValueExpr, ValueExpr> mapValues) {
            for(Map.Entry<ValueExpr,ValueExpr> mapValue : mapValues.entrySet())
                if(!(mapValue.getKey().objectClass.equals(mapValue.getValue().objectClass)))
                    return null;
            return mapValues;
        }

        protected Iterator<Map<ValueExpr, ValueExpr>> mapIterator() {
            return new Pairs<ValueExpr,ValueExpr>(values1, values2).iterator();
        }
    }

    private class MapIterator implements Iterator<KeyTranslator> {
        private Pairs<KeyExpr,KeyExpr> keyPairs;

        private Iterator<Map<ValueExpr,ValueExpr>> valueIterator;
        private Iterator<Map<KeyExpr,KeyExpr>> keysIterator;

        public MapIterator() {
            mapValues = null;
            if(values) {
                valueIterator = new ValuePairs(from.getContext().values,to.getContext().values).iterator();
                if(valueIterator.hasNext())
                    mapValues = valueIterator.next();
            } else {
                valueIterator = new EmptyIterator<Map<ValueExpr, ValueExpr>>();
                if(from.getContext().values.equals(to.getContext().values))
                    mapValues = BaseUtils.toMap(from.getContext().values);
            }

            if(mapValues==null)
                keysIterator = new EmptyIterator<Map<KeyExpr, KeyExpr>>();
            else {
                keyPairs = new Pairs<KeyExpr,KeyExpr>(from.getContext().keys,to.getContext().keys);
                keysIterator = keyPairs.iterator();
            }
        }

        public boolean hasNext() {
            return keysIterator.hasNext() || valueIterator.hasNext();
        }

        private Map<ValueExpr,ValueExpr> mapValues;

        public KeyTranslator next() {
            Map<KeyExpr,KeyExpr> mapKeys;
            if(keysIterator.hasNext())
                mapKeys = keysIterator.next();
            else {
                mapValues = valueIterator.next();
                keysIterator = keyPairs.iterator();
                mapKeys = keysIterator.next();
            }
            return new KeyTranslator(mapKeys,mapValues);
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }
}

package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.EmptyIterator;
import platform.base.Pairs;
import platform.server.caches.HashContext;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.KeyTranslator;

import java.util.Iterator;
import java.util.Map;

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

    // перебирает Map'ы KeyExpr -> KeyExpr и ValueExpr -> ValueExpr
    private class MapIterator implements Iterator<KeyTranslator> {
        private Pairs<KeyExpr,KeyExpr> keyPairs;

        private Iterator<Map<ValueExpr,ValueExpr>> valueIterator;
        private Iterator<Map<KeyExpr,KeyExpr>> keysIterator;

        public MapIterator() {
            mapValues = null;
            if(values) {
                valueIterator = new ValuePairs(from.getValues(),to.getValues()).iterator();
                if(valueIterator.hasNext())
                    mapValues = valueIterator.next();
            } else {
                valueIterator = new EmptyIterator<Map<ValueExpr, ValueExpr>>();
                if(from.getValues().equals(to.getValues())) // если контексты не совпадают то сразу вываливаемся
                    mapValues = BaseUtils.toMap(from.getValues());
            }

            if(mapValues==null)
                keysIterator = new EmptyIterator<Map<KeyExpr, KeyExpr>>();
            else {
                keyPairs = new Pairs<KeyExpr,KeyExpr>(from.getKeys(),to.getKeys());
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

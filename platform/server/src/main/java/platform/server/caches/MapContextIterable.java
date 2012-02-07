package platform.server.caches;

import platform.base.BaseUtils;
import platform.base.EmptyIterator;
import platform.base.GlobalObject;
import platform.base.QuickMap;
import platform.server.caches.hash.HashCodeValues;
import platform.server.caches.hash.HashMapValues;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.MapTranslator;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;

import java.util.Iterator;
import java.util.Map;

public class MapContextIterable implements Iterable<MapTranslate> {

    private final InnerContext<?> from;
    private final InnerContext<?> to;
    private final boolean values;

    public MapContextIterable(InnerContext from, InnerContext to, boolean values) {
        this.from = from;
        this.to = to;
        this.values = values;
    }

    public Iterator<MapTranslate> iterator() {
        return new MapIterator();
    }

    // перебирает Map'ы KeyExpr -> KeyExpr и ValueExpr -> ValueExpr
    private class MapIterator implements Iterator<MapTranslate> {
        private KeyPairs keyPairs;

        private Iterator<MapValuesTranslate> valueIterator;
        private Iterator<Map<KeyExpr,KeyExpr>> keysIterator;

        public MapIterator() {
            mapValues = null;
            if(values) {
                valueIterator = MapValuesIterable.mapIterator(from, to);
                if(valueIterator.hasNext())
                    mapValues = valueIterator.next();
            } else {
                valueIterator = new EmptyIterator<MapValuesTranslate>();
                if(from.getInnerValues().equals(to.getInnerValues())) // если контексты не совпадают то сразу вываливаемся
                    mapValues = MapValuesTranslator.noTranslate;
            }

            if(mapValues==null) {
                keysIterator = new EmptyIterator<Map<KeyExpr, KeyExpr>>();
                return;
            }

            keyPairs = new KeyPairs(from.getInnerComponents(values).map,to.getInnerComponents(values).map);
            keysIterator = keyPairs.iterator();
            if(!keysIterator.hasNext())
                valueIterator = new EmptyIterator<MapValuesTranslate>();
        }

        public boolean hasNext() {
            return keysIterator.hasNext() || valueIterator.hasNext();
        }

        private MapValuesTranslate mapValues;

        public MapTranslate next() {
            Map<KeyExpr,KeyExpr> mapKeys;
            if(!keysIterator.hasNext()) {
                mapValues = valueIterator.next();
                keysIterator = keyPairs.iterator();
            }
            mapKeys = keysIterator.next();
            return new MapTranslator(mapKeys,mapValues);
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }
}

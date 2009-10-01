package platform.server.caches;

import platform.base.MapIterable;
import platform.server.data.query.HashContext;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.translators.KeyTranslator;

import java.util.Iterator;

public class MapHashIterable extends MapIterable<KeyTranslator,KeyTranslator> {

    private final MapContext from;
    private final MapContext to;
    private final boolean values;

    public MapHashIterable(MapContext from, MapContext to, boolean values) {
        this.from = from;
        this.to = to;
        this.values = values;
    }

    protected KeyTranslator map(final KeyTranslator translator) {
        if(from.hash(new HashContext(){
            public int hash(KeyExpr expr) {
                return translator.keys.get(expr).hashCode();
            }
            public int hash(ValueExpr expr) {
                return (values?translator.values.get(expr):expr).hashCode();
            }
        })==to.hash(new HashContext(){
            public int hash(KeyExpr expr) {
                return expr.hashCode();
            }
            public int hash(ValueExpr expr) {
                return expr.hashCode();
            }
        }))
            return translator;
        else
            return null;
    }

    protected Iterator<KeyTranslator> mapIterator() {
        return new MapParamsIterable(from, to, values).iterator();
    }
}

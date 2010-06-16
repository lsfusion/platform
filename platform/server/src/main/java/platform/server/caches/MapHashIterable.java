package platform.server.caches;

import platform.base.MapIterable;
import platform.server.caches.hash.HashCodeContext;
import platform.server.caches.hash.HashTranslateContext;
import platform.server.data.translator.MapTranslate;

import java.util.Iterator;

public class MapHashIterable extends MapIterable<MapTranslate, MapTranslate> {

    private final MapContext from;
    private final MapContext to;
    private final boolean values;

    public MapHashIterable(MapContext from, MapContext to, boolean values) {
        this.from = from;
        this.to = to;
        this.values = values;
    }

    protected MapTranslate map(final MapTranslate translator) {
        if(from.hash(new HashTranslateContext(translator, values))==to.hash(HashCodeContext.instance))
            return translator;
        else
            return null;
    }

    protected Iterator<MapTranslate> mapIterator() {
        return new MapParamsIterable(from, to, values).iterator();
    }
}

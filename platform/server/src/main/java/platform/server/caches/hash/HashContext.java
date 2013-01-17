package platform.server.caches.hash;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

public class HashContext extends HashObject {

    public static HashContext create(HashKeys keys, HashValues values) {
        if(keys == HashCodeKeys.instance && values == HashCodeValues.instance)
            return hashCode;
        return new HashContext(keys, values);
    }

    public final HashKeys keys;
    public final HashValues values;

    public HashContext(HashKeys keys, HashValues values) {
        this.keys = keys;
        this.values = values;
    }

    public boolean isGlobal() {
        return keys.isGlobal() && values.isGlobal();
    }

    public final static HashContext hashCode = new HashContext(HashCodeKeys.instance,HashCodeValues.instance);

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof HashContext && keys.equals(((HashContext) o).keys) && values.equals(((HashContext) o).values);
    }

    @Override
    public int hashCode() {
        return 31 * keys.hashCode() + values.hashCode();
    }

    public HashContext filterKeysValues(ImSet<KeyExpr> filterKeys, ImSet<Value> filterValues) {
        return HashContext.create(keys.filterKeys(filterKeys), values.filterValues(filterValues));
    }

    public HashContext reverseTranslate(MapTranslate translator, ImSet<KeyExpr> contextKeys, ImSet<Value> contextValues) {
        HashKeys transKeys = keys.reverseTranslate(translator, contextKeys);
        if(transKeys==null)
            return null;
        HashValues transValues = values.reverseTranslate(translator.mapValues(), contextValues);
        if(transValues==null)
            return null;
        return HashContext.create(transKeys, transValues);
    }
}

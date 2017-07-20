package lsfusion.server.caches.hash;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapTranslate;

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

    public HashContext filterKeysValues(ImSet<ParamExpr> filterKeys, ImSet<Value> filterValues) {
        return HashContext.create(keys.filterKeys(filterKeys), values.filterValues(filterValues));
    }

    public HashContext reverseTranslate(MapTranslate translator, ImSet<ParamExpr> contextKeys, ImSet<Value> contextValues) {
        HashKeys transKeys = keys.reverseTranslate(translator, contextKeys);
        if(transKeys==null)
            return null;
        HashValues transValues = values.reverseTranslate(translator.mapValues(), contextValues);
        if(transValues==null)
            return null;
        return HashContext.create(transKeys, transValues);
    }
}

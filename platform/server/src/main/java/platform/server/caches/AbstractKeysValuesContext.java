package platform.server.caches;

import platform.base.col.interfaces.immutable.ImSet;
import platform.server.caches.hash.HashContext;
import platform.server.data.Value;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.MapTranslate;

public abstract class AbstractKeysValuesContext<T> extends AbstractTranslateContext<T, MapTranslate, HashContext> {

    protected HashContext reverseTranslate(HashContext hash, MapTranslate translator) {
        return hash.reverseTranslate(translator, aspectGetKeys(), aspectGetValues());
    }

    @Override
    protected T aspectContextTranslate(MapTranslate translator) {
        ImSet<Value> values = aspectGetValues();
        ImSet<KeyExpr> keys = aspectGetKeys();

        ImSet<KeyExpr> transKeys = translator.translateKeys(keys);
        ImSet<Value> transValues = translator.translateValues(values);

        if(transValues.equals(values) && transKeys.equals(keys))
            return (T) this;
        else {
            AbstractKeysValuesContext<T> result = (AbstractKeysValuesContext<T>) translate(translator);
            result.values = transValues;
            result.keys = transKeys;
            return (T) result;
        }
    }

    protected HashContext aspectContextHash(HashContext hash) {
        return hash.filterKeysValues(aspectGetKeys(), aspectGetValues());
    }

    private ImSet<KeyExpr> keys;
    @ManualLazy
    protected ImSet<KeyExpr> aspectGetKeys() {
        if(keys==null)
            keys = getKeys();
        return keys;
    }
    protected abstract ImSet<KeyExpr> getKeys();
}

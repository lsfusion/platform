package lsfusion.server.caches;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.Value;
import lsfusion.server.data.translator.MapTranslate;

public abstract class AbstractKeysValuesContext<T> extends AbstractTranslateContext<T, MapTranslate, HashContext> {

    protected HashContext reverseTranslate(HashContext hash, MapTranslate translator) {
        return hash.reverseTranslate(translator, aspectGetKeys(), aspectGetValues());
    }

    @Override
    protected T aspectContextTranslate(MapTranslate translator) {
        ImSet<Value> values = aspectGetValues();
        ImSet<ParamExpr> keys = aspectGetKeys();

        ImSet<ParamExpr> transKeys = translator.translateDirect(keys);
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

    private ImSet<ParamExpr> keys;
    @ManualLazy
    protected ImSet<ParamExpr> aspectGetKeys() {
        if(keys==null)
            keys = getKeys();
        return keys;
    }
    protected abstract ImSet<ParamExpr> getKeys();
}

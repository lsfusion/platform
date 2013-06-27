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

        if(translator.identityKeysValues(keys, values)) // если identity трансляция
            return (T) this;
        else {
            AbstractKeysValuesContext<T> result = (AbstractKeysValuesContext<T>) translate(translator);
            result.values = translator.translateValues(values);
            result.keys = translator.translateDirect(keys);
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

package platform.server.caches.hash;

import platform.server.caches.NoCacheInterface;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapTranslate;

public class HashTranslateContext implements HashContext, NoCacheInterface {

    private final MapTranslate translator;
    private final boolean values;

    public HashTranslateContext(MapTranslate translator, boolean values) {
        this.translator = translator;
        this.values = values;
    }

    public int hash(KeyExpr expr) {
        return translator.translate(expr).hashCode();
    }

    public int hash(ValueExpr expr) {
        return (values?translator.translate(expr):expr).hashCode();
    }

    public HashContext mapKeys() {
        if(values)
            return new HashTranslateValuesContext(translator.mapValues());
        else
            return HashMapKeysContext.instance;
    }
}

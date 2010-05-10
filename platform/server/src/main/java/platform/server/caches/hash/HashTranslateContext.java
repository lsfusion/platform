package platform.server.caches.hash;

import platform.server.data.translator.DirectTranslator;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.caches.NoCacheInterface;

public class HashTranslateContext implements HashContext, NoCacheInterface {

    private final DirectTranslator translator;
    private final boolean values;

    public HashTranslateContext(DirectTranslator translator, boolean values) {
        this.translator = translator;
        this.values = values;
    }

    public int hash(KeyExpr expr) {
        return translator.translate(expr).hashCode();
    }

    public int hash(ValueExpr expr) {
        return (values?translator.values.get(expr):expr).hashCode();
    }

    public HashContext mapKeys() {
        if(values)
            return new HashTranslateValuesContext(translator.values);
        else
            return HashMapKeysContext.instance;
    }
}

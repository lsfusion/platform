package platform.server.caches;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

public abstract class AbstractTranslateContext<This extends TranslateContext> implements TranslateContext<This> {

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashContext(new HashContext() {
                public int hash(KeyExpr expr) {
                    return expr.hashCode();
                }
                public int hash(ValueExpr expr) {
                    return expr.hashCode();
                }
            });
            hashCoded = true;
        }
        return hashCode;
    }

}

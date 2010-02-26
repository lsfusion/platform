package platform.server.caches;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;

public abstract class AbstractMapValues<U extends AbstractMapValues<U>> implements MapValues<U>  {

    boolean hashCoded = false;
    int hashCode;
    @Override
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashValues(new HashValues() {
                public int hash(ValueExpr expr) {
                    return expr.hashCode();
                }
            });
            hashCoded = true;
        }
        return hashCode;
    }


}

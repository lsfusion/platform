package platform.server.data.expr.where.extra;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.HashLazy;
import platform.server.data.where.Where;

// если operator1 не null и больше operator2 или operator2 null
public class GreaterWhere extends CompareWhere {

    // public только для symmetricWhere
    public GreaterWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(BaseUtils.hashEquals(operator1,operator2))
            return FALSE;
        return create(operator1, operator2, new GreaterWhere(operator1, operator2));
    }

    @HashLazy
    public int hashOuter(HashContext hashContext) {
        return 1 + operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31*31;
    }

    protected CompareWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new GreaterWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.GREATER;
    }

    protected String getCompareSource(CompileSource compile) {
        return ">";
    }
}

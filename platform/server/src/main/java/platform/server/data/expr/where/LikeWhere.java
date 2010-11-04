package platform.server.data.expr.where;

import platform.server.data.where.DataWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.caches.hash.HashContext;
import platform.server.caches.IdentityLazy;
import platform.interop.Compare;
import platform.base.BaseUtils;

public class LikeWhere extends BinaryWhere<LikeWhere> {

    private LikeWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    protected LikeWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new LikeWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.LIKE;
    }

    @IdentityLazy
    public int hashOuter(HashContext hashContext) {
        return 1 + operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31*31;
    }

    protected String getCompareSource(CompileSource compile) {
        return " LIKE ";
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        return create(new LikeWhere(operator1, operator2));
    }
}

package platform.server.data.expr.query;

import platform.base.BaseUtils;
import platform.server.caches.ParamLazy;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;

import java.util.Collections;
import java.util.Map;

public class MaxGroupExpr extends GroupExpr {

    protected MaxGroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        super(group, expr);
    }

    public MaxGroupExpr(MaxGroupExpr maxExpr, KeyTranslator translator) {
        super(maxExpr, translator);
    }

    @ParamLazy
    public MaxGroupExpr translateDirect(KeyTranslator translator) {
        return new MaxGroupExpr(this,translator);
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    private BaseExpr getBaseExpr() {
        return BaseUtils.single(query.getCases()).data;
    }
    
    protected class NotNull extends GroupExpr.NotNull {

        protected ClassExprWhere getClassWhere(Where fullWhere) {
            return fullWhere.getClassWhere().map(BaseUtils.merge(Collections.singletonMap(getBaseExpr(), MaxGroupExpr.this), group));
        }
    }

    public boolean isMax() {
        return true;
    }
}

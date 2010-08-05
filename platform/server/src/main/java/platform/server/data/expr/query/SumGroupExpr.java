package platform.server.data.expr.query;

import platform.server.caches.ParamLazy;
import platform.server.classes.IntegralClass;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Map;

public class SumGroupExpr extends GroupExpr {

    protected SumGroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        super(group, expr);
    }

    private SumGroupExpr(SumGroupExpr sumExpr, MapTranslate translator) {
        super(sumExpr, translator);
    }    
    @ParamLazy
    public SumGroupExpr translateOuter(MapTranslate translator) {
        return new SumGroupExpr(this, translator); 
    }

    protected SumGroupExpr createThis(Expr query, Map<BaseExpr, BaseExpr> group) {
        return new SumGroupExpr(group, query);
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    protected class NotNull extends GroupExpr.NotNull {

        protected ClassExprWhere getClassWhere(Where fullWhere) {
            return fullWhere.getClassWhere().map(group).and(new ClassExprWhere(SumGroupExpr.this,(IntegralClass) query.getType(fullWhere)));
        }
    }

    public boolean isMax() {
        return false;
    }
}

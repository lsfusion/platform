package platform.server.data.expr.query;

import platform.server.caches.ParamLazy;
import platform.server.classes.IntegralClass;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.VariableClassExpr;

import java.util.Map;

public class SumGroupExpr extends GroupExpr {

    protected SumGroupExpr(Map<BaseExpr, BaseExpr> group, Expr expr) {
        super(group, expr);
    }

    private SumGroupExpr(SumGroupExpr sumExpr, KeyTranslator translator) {
        super(sumExpr, translator);
    }    
    @ParamLazy
    public VariableClassExpr translateDirect(KeyTranslator translator) {
        return new SumGroupExpr(this, translator); 
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

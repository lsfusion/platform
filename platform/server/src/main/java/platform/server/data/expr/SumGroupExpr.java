package platform.server.data.expr;

import platform.server.caches.ParamLazy;
import platform.server.classes.IntegralClass;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;

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
            return fullWhere.getClassWhere().map(group).and(new ClassExprWhere(SumGroupExpr.this,(IntegralClass)expr.getType(fullWhere)));
        }
    }

    public boolean isMax() {
        return false;
    }
}

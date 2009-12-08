package platform.server.data.expr;

import platform.server.caches.ParamLazy;
import platform.server.classes.IntegralClass;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.where.Where;
import platform.server.data.type.Type;

import java.util.Map;
import java.util.HashMap;

public class SumGroupExpr extends GroupExpr<Expr,SumGroupExpr> {

    private SumGroupExpr(Map<AndExpr, AndExpr> group, Where where, Expr expr, Where upWhere) {
        super(group, where, expr, upWhere, new HashMap<KeyExpr, Type>());
    }
    public static Expr create(Map<AndExpr, AndExpr> group, Where where, Expr expr) {
        return new SumGroupExpr(group, where, expr, Where.TRUE).packCreate();
    }

    private SumGroupExpr(Map<AndExpr, AndExpr> group, Where where, Expr expr) {
        super(group, where, expr);
    }
    protected GroupExpr createPacked(Map<AndExpr, AndExpr> group, Where<?> where, Expr expr) {
        return new SumGroupExpr(group, where, expr);
    }

    public Expr packExpr(Expr expr, Where trueWhere) {
        return expr.followFalse(trueWhere.not());
    }

    public SumGroupExpr(SumGroupExpr sumExpr, KeyTranslator translator) {
        super(sumExpr, translator);
    }
    
    @ParamLazy
    public VariableClassExpr translateDirect(KeyTranslator translator) {
        return new SumGroupExpr(this, translator); 
    }

    public SumGroupExpr(SumGroupExpr groupExpr, Where falseWhere) {
        super(groupExpr, falseWhere);
    }
    @Override
    public AndExpr packFollowFalse(Where where) {
        return new SumGroupExpr(this, where);
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    protected class NotNull extends GroupExpr.NotNull {

        public ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; 
            return fullWhere.getClassWhere().map(group).and(new ClassExprWhere(SumGroupExpr.this,(IntegralClass)expr.getType(fullWhere))).and(getJoinsWhere(group).getClassWhere());
        }
    }

    protected Expr createThis(Where iWhere, Map<AndExpr, AndExpr> iGroup, Expr iExpr) {
        return create(iGroup, iWhere, iExpr);
    }

    public boolean isMax() {
        return false;
    }
}

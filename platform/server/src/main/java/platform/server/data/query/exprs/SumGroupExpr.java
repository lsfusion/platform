package platform.server.data.query.exprs;

import platform.server.data.classes.IntegralClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.where.Where;
import platform.server.caches.ParamLazy;

import java.util.Map;

public class SumGroupExpr extends GroupExpr<SourceExpr,SumGroupExpr> {

    private SumGroupExpr(Where iWhere, Map<AndExpr, AndExpr> iGroup, SourceExpr iExpr) {
        super(iWhere, iGroup, iExpr);
    }
    public static SourceExpr create(Where iWhere, Map<AndExpr, AndExpr> iGroup, SourceExpr iExpr) {
        return AndExpr.create(new SumGroupExpr(iWhere, iGroup, iExpr));
    }

    public SourceExpr packExpr(SourceExpr expr, Where trueWhere) {
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

    protected Where calculateWhere() {
        return new NotNull();
    }

    protected class NotNull extends GroupExpr.NotNull {

        protected ClassExprWhere calculateClassWhere() {
            Where fullWhere = getFullWhere();
            if(fullWhere.isFalse()) return ClassExprWhere.FALSE; 
            return fullWhere.getClassWhere().map(group).and(new ClassExprWhere(SumGroupExpr.this,(IntegralClass)expr.getType(fullWhere))).and(getJoinsWhere(group).getClassWhere());
        }
    }

    protected SourceExpr createThis(Where iWhere, Map<AndExpr, AndExpr> iGroup, SourceExpr iExpr) {
        return create(iWhere, iGroup, iExpr);
    }

    public boolean isMax() {
        return false;
    }
}

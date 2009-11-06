package platform.server.data.query.exprs;

import platform.server.caches.ParamLazy;
import platform.server.data.classes.IntegralClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.where.Where;

import java.util.Map;

public class SumGroupExpr extends GroupExpr<SourceExpr,SumGroupExpr> {

    private SumGroupExpr(Map<AndExpr, AndExpr> group, Where where, SourceExpr expr, Where upWhere) {
        super(group, where, expr, upWhere);
    }
    public static SourceExpr create(Map<AndExpr, AndExpr> group, Where where, SourceExpr expr) {
        return new SumGroupExpr(group, where, expr, Where.TRUE).packCreate();
    }

    private SumGroupExpr(Map<AndExpr, AndExpr> group, Where where, SourceExpr expr) {
        super(group, where, expr);
    }
    protected GroupExpr createPacked(Map<AndExpr, AndExpr> group, Where<?> where, SourceExpr expr) {
        return new SumGroupExpr(group, where, expr);
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

    protected SourceExpr createThis(Where iWhere, Map<AndExpr, AndExpr> iGroup, SourceExpr iExpr) {
        return create(iGroup, iWhere, iExpr);
    }

    public boolean isMax() {
        return false;
    }
}

package platform.server.data.query.exprs;

import platform.base.BaseUtils;
import platform.server.caches.ParamLazy;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.where.Where;

import java.util.Collections;
import java.util.Map;

public class MaxGroupExpr extends GroupExpr<AndExpr,MaxGroupExpr> {

    private MaxGroupExpr(Map<AndExpr, AndExpr> group, Where where, AndExpr expr, Where upWhere) {
        super(group, where, expr, upWhere);
    }
    public static SourceExpr create(Map<AndExpr, AndExpr> group, Where where, AndExpr expr) {
        return new MaxGroupExpr(group, where, expr, Where.TRUE).packCreate();
    }

    private MaxGroupExpr(Map<AndExpr, AndExpr> group, Where where, AndExpr expr) {
        super(group, where, expr);
    }
    protected GroupExpr createPacked(Map<AndExpr, AndExpr> group, Where<?> where, AndExpr expr) {
        return new MaxGroupExpr(group, where, expr);
    }

    public MaxGroupExpr(MaxGroupExpr maxExpr, KeyTranslator translator) {
        super(maxExpr, translator);
    }

    @ParamLazy
    public MaxGroupExpr translateDirect(KeyTranslator translator) {
        return new MaxGroupExpr(this,translator);
    }

    public MaxGroupExpr(MaxGroupExpr maxExpr, Where packWhere) {
        super(maxExpr, packWhere);
    }
    @Override
    public AndExpr packFollowFalse(Where where) {
        return new MaxGroupExpr(this, where);
    }

    public Where calculateWhere() {
        return new NotNull();
    }

    protected SourceExpr createThis(Where iWhere, Map<AndExpr, AndExpr> iGroup, AndExpr iExpr) {
        return create(iGroup, iWhere, iExpr);
    }

    public AndExpr packExpr(AndExpr expr, Where trueWhere) {
        return expr.packFollowFalse(trueWhere.not());
    }

    protected class NotNull extends GroupExpr.NotNull {

        public ClassExprWhere calculateClassWhere() {
            return getFullWhere().getClassWhere().map(BaseUtils.merge(Collections.singletonMap(expr, MaxGroupExpr.this), group)).and(getJoinsWhere(group).getClassWhere());
        }
    }

    public boolean isMax() {
        return true;
    }
}

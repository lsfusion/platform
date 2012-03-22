package platform.server.data.query;

import platform.base.QuickSet;
import platform.base.Result;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.OuterContext;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.InnerExpr;
import platform.server.data.expr.NotNullExpr;
import platform.server.data.expr.NotNullExprSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.Map;

public abstract class ExprJoin<T extends ExprJoin<T>> extends AbstractOuterContext<T> implements WhereJoin<Integer, T> {

    protected final BaseExpr baseExpr;

    public ExprJoin(BaseExpr baseExpr) {
        this.baseExpr = baseExpr;
    }

    @Override
    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(baseExpr);
    }

    protected int hash(HashContext hashContext) {
        return baseExpr.hashOuter(hashContext);
    }

    public boolean twins(TwinImmutableInterface o) {
        return baseExpr.equals(((ExprJoin)o).baseExpr);
    }

    public InnerJoins getJoinFollows(Result<Map<InnerJoin, Where>> upWheres) { // все равно использует getExprFollows
        return InnerExpr.getFollowJoins(this, upWheres);
    }

    public NotNullExprSet getExprFollows(boolean recursive) {
        return InnerExpr.getExprFollows(this, recursive);
    }

    public Map<Integer, BaseExpr> getJoins() {
        return Collections.singletonMap(0, (BaseExpr)baseExpr);
    }

    public InnerJoins getInnerJoins() {
        InnerJoins result = new InnerJoins();
        NotNullExprSet notNullExprs = baseExpr.getExprFollows(true, false);
        for(int i=0;i<notNullExprs.size;i++) {
            NotNullExpr notNullExpr = notNullExprs.get(i);
            if(notNullExpr instanceof InnerExpr)
                result = result.and(new InnerJoins(((InnerExpr)notNullExpr).getInnerJoin()));
        }
        return result;
    }
}

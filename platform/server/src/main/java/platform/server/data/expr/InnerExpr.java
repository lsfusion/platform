package platform.server.data.expr;

import platform.base.Result;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.BaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.UnionJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.Where;

public abstract class InnerExpr extends NotNullExpr implements JoinData {

    public void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        joins.add(this, andWhere);
    }

    public InnerJoin<?, ?> getFJGroup() {
        return getInnerJoin();
    }

    public Expr getFJExpr() {
        return this;
    }

    public String getFJString(String exprFJ) {
        return exprFJ;
    }

    public abstract class NotNull extends NotNullExpr.NotNull {

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, boolean noWhere) {
            return new GroupJoinsWheres(InnerExpr.this.getInnerJoin(), this, noWhere);
        }
    }

    // множественное наследование
    public static <K> ImSet<NotNullExpr> getExprFollows(BaseJoin<K> join, boolean recursive) { // куда-то надо же положить
        return getExprFollows(join.getJoins().values(), recursive);
    }

    // множественное наследование
    public static InnerJoins getInnerJoins(InnerJoin join) { // куда-то надо же положить
        return new InnerJoins(join);
    }

    // множественное наследование
    public static InnerJoins getFollowJoins(WhereJoin<?, ?> join, Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // куда-то надо же положить
        InnerJoins result = new InnerJoins();
        ImMap<InnerJoin, Where> upResult = MapFact.EMPTY();
        ImSet<InnerExpr> innerExprs = getInnerExprs(join.getExprFollows(false), unionJoins);
        for(int i=0,size=innerExprs.size();i<size;i++) {
            InnerExpr innerExpr = innerExprs.get(i);
            InnerJoin innerJoin = innerExpr.getInnerJoin();
            result = result.and(new InnerJoins(innerJoin));
            upResult = result.andUpWheres(upResult, MapFact.singleton(innerJoin, innerExpr.getWhere()));
        }
        upWheres.set(upResult);
        return result;
    }

    public abstract InnerJoin<?, ?> getInnerJoin();
    public InnerJoin<?, ?> getBaseJoin() {
        return getInnerJoin();
    }

    protected abstract InnerExpr translate(MapTranslate translator);
}

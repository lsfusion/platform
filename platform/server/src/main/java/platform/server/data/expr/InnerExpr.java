package platform.server.data.expr;

import platform.base.QuickSet;
import platform.base.Result;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.*;
import platform.server.data.query.stat.BaseJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.where.Where;

import java.util.*;

public abstract class InnerExpr extends NotNullExpr implements JoinData {

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
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

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat, List<Expr> orderTop, boolean noWhere) {
            return new GroupJoinsWheres(InnerExpr.this.getInnerJoin(), this, noWhere);
        }
    }

    // множественное наследование
    public static <K> NotNullExprSet getExprFollows(BaseJoin<K> join, boolean recursive) { // куда-то надо же положить
        return new NotNullExprSet(join.getJoins().values(), recursive);
    }

    // множественное наследование
    public static InnerJoins getInnerJoins(InnerJoin join) { // куда-то надо же положить
        return new InnerJoins(join);
    }

    // множественное наследование
    public static InnerJoins getFollowJoins(WhereJoin<?, ?> join, Result<Map<InnerJoin, Where>> upWheres, Collection<UnionJoin> unionJoins) { // куда-то надо же положить
        InnerJoins result = new InnerJoins();
        Map<InnerJoin, Where> upResult = new HashMap<InnerJoin, Where>();
        QuickSet<InnerExpr> innerExprs = join.getExprFollows(false).getInnerExprs(unionJoins);
        for(int i=0;i<innerExprs.size;i++) {
            InnerExpr innerExpr = innerExprs.get(i);
            InnerJoin innerJoin = innerExpr.getInnerJoin();
            result = result.and(new InnerJoins(innerJoin));
            upResult = result.andUpWheres(upResult, Collections.singletonMap(innerJoin,  innerExpr.getWhere()));
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

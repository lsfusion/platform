package platform.server.data.expr;

import platform.base.QuickSet;
import platform.base.Result;
import platform.server.caches.ManualLazy;
import platform.server.data.query.InnerJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.*;
import platform.server.data.query.stat.BaseJoin;
import platform.server.data.translator.MapTranslate;
import platform.server.data.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(QuickSet<K> keepStat, KeyStat keyStat) {
            return new GroupJoinsWheres(InnerExpr.this.getInnerJoin(), this);
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
    public static InnerJoins getFollowJoins(WhereJoin<?, ?> join, Result<Map<InnerJoin, Where>> upWheres) { // куда-то надо же положить
        InnerJoins result = new InnerJoins();
        Map<InnerJoin, Where> upResult = new HashMap<InnerJoin, Where>();
        NotNullExprSet notNullExprs = join.getExprFollows(false);
        for(int i=0;i<notNullExprs.size;i++) {
            NotNullExpr notNullExpr = notNullExprs.get(i);
            if(notNullExpr instanceof InnerExpr) {
                InnerJoin innerJoin = ((InnerExpr)notNullExpr).getInnerJoin();
                result = result.and(new InnerJoins(innerJoin));
                upResult = result.andUpWheres(upResult, Collections.singletonMap(innerJoin,  notNullExpr.getWhere()));
            }
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

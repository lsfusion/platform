package lsfusion.server.data.expr;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.ExprStatJoin;
import lsfusion.server.data.query.InnerJoin;
import lsfusion.server.data.query.InnerJoins;
import lsfusion.server.data.query.JoinData;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.stat.BaseJoin;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.UnionJoin;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

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
            WhereJoin join = InnerExpr.this.getInnerJoin();

            PropStat statValue = InnerExpr.this.getStatValue(keyStat);
            if(statValue.notNull!=null && statValue.notNull.less(join.getStatKeys(keyStat).rows))
                join = new ExprStatJoin(InnerExpr.this, statValue.notNull); // сама статистика тут не важна, важно наличие join'а

            return new GroupJoinsWheres(join, this, noWhere);
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
        InnerJoins result = InnerJoins.EMPTY;
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

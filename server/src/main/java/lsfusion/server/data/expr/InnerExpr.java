package lsfusion.server.data.expr;

import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.query.*;
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

        @Override
        protected String getNotSource(CompileSource compile) {
            return compile.getNullSource(InnerExpr.this, super.getNotSource(compile));
        }

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
            WhereJoin join = InnerExpr.this.getInnerJoin();

            PropStat statValue = InnerExpr.this.getStatValue(keyStat);
            if(statValue.notNull!=null && statValue.notNull.less(join.getStatKeys(keyStat).rows))
                join = new ExprStatJoin(InnerExpr.this, statValue.notNull); // сама статистика тут не важна, важно наличие join'а

            return new GroupJoinsWheres(join, this, type);
        }
    }

    private static <K> InnerFollows<K> getInnerFollows(BaseJoin<K> join) {
        if(join instanceof InnerJoin)
            return ((InnerJoin<K, ?>) join).getInnerFollows();
        return InnerFollows.EMPTY();
    }
    // множественное наследование
    public static <K> ImSet<NotNullExprInterface> getExprFollows(BaseJoin<K> join, boolean includeInnerWithoutNotNull, boolean recursive) { // куда-то надо же положить
        return getInnerFollows(join).getExprFollows(join.getJoins(), includeInnerWithoutNotNull, recursive);
    }
    public static <K> boolean hasExprFollowsWithoutNotNull(BaseJoin<K> join) { // куда-то надо же положить, проверяет "нужен" ли параметр includeInnerWithoutNotNull или можно считать его равным false
        return getInnerFollows(join).hasExprFollowsWithoutNotNull(join.getJoins());
    }

    // множественное наследование
    public static InnerJoins getInnerJoins(InnerJoin join) { // куда-то надо же положить
        return new InnerJoins(join);
    }

    // множественное наследование
    public static InnerJoins getJoinFollows(BaseJoin<?> join, Result<ImMap<InnerJoin, Where>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // куда-то надо же положить
        InnerJoins result = InnerJoins.EMPTY;
        ImMap<InnerJoin, Where> upResult = MapFact.EMPTY();
        ImSet<InnerExpr> innerExprs = getInnerExprs(join.getExprFollows(NotNullExpr.INNERJOINS, false), unionJoins);
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

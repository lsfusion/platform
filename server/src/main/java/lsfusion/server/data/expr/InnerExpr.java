package lsfusion.server.data.expr;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.query.PropStat;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.query.StatType;
import lsfusion.server.data.query.*;
import lsfusion.server.data.query.innerjoins.GroupJoinsWheres;
import lsfusion.server.data.query.innerjoins.InnerUpWhere;
import lsfusion.server.data.query.innerjoins.UpWhere;
import lsfusion.server.data.query.innerjoins.UpWheres;
import lsfusion.server.data.query.stat.*;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.where.Where;

public abstract class InnerExpr extends NullableExpr implements JoinData {

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

    public abstract class NotNull extends NullableExpr.NotNull {

        @Override
        protected String getNotSource(CompileSource compile) {
            return compile.getNullSource(InnerExpr.this, super.getNotSource(compile));
        }

        public <K extends BaseExpr> GroupJoinsWheres groupJoinsWheres(ImSet<K> keepStat, StatType statType, KeyStat keyStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
            WhereJoin join = InnerExpr.this.getInnerJoin();

            WhereJoin notNullJoin = getNotNullJoin(keyStat, statType);
            if(notNullJoin != null)
                join = notNullJoin;

            return groupDataJoinsWheres(join, type);
        }
    }

    public UpWhere getUpNotNullWhere() {
        return new InnerUpWhere(this);
    }

    protected abstract PropStat getInnerStatValue(KeyStat keyStat, StatType type);

    public PropStat getStatValue(KeyStat keyStat, StatType type) {
        return getInnerStatValue(keyStat, type);
    }

    public ExprStatJoin getAdjustStatJoin(Stat desiredJoinStat, KeyStat keyStat, StatType statType, boolean notNull) {
        Stat statValue = getInnerStatValue(keyStat, statType).distinct;
        Stat joinStats = getInnerStatRows(statType);
//        if (desiredJoinStat.less(joinStats)) // оптимизация
        return new ExprStatJoin(this, statValue, desiredJoinStat, joinStats, false);
//        return null;
    }

    public ExprStatJoin getNotNullJoin(KeyStat keyStat, StatType statType) {
        if (this instanceof Table.Join.Expr) { // нет смысла вызывать getInnerStatValue лишний раз для QueryJoin {
            PropStat statValue = getInnerStatValue(keyStat, statType);
            if (statValue.notNull != null) {
                assert this instanceof Table.Join.Expr;
                Stat joinStats = getInnerStatRows(statType);
                if (statValue.notNull.less(joinStats))
                    return new ExprStatJoin(this, statValue.notNull, true);
    //            return getAdjustStatJoin(statValue.notNull, keyStat, statType, true);
            }
        }
        return null;
    }

    private static <K> InnerFollows<K> getInnerFollows(BaseJoin<K> join) {
        if(join instanceof InnerJoin)
            return  ((InnerJoin<K, ?>) join).getInnerFollows();
        return InnerFollows.EMPTY();
    }
    // множественное наследование
    public static <K> ImSet<NullableExprInterface> getExprFollows(BaseJoin<K> join, boolean includeInnerWithoutNotNull, boolean recursive) { // куда-то надо же положить
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
    public static InnerJoins getJoinFollows(BaseJoin<?> join, Result<UpWheres<InnerJoin>> upWheres, Result<ImSet<UnionJoin>> unionJoins) { // куда-то надо же положить
        InnerJoins result = InnerJoins.EMPTY;
        UpWheres<InnerJoin> upResult = UpWheres.EMPTY();
        ImSet<InnerExpr> innerExprs = getInnerExprs(join.getExprFollows(NullableExpr.INNERJOINS, false), unionJoins);
        for(int i=0,size=innerExprs.size();i<size;i++) {
            InnerExpr innerExpr = innerExprs.get(i);
            InnerJoin innerJoin = innerExpr.getInnerJoin();
            result = result.and(new InnerJoins(innerJoin));
            upResult = result.andUpWheres(upResult, new UpWheres<>(innerJoin, innerExpr.getUpNotNullWhere()));
        }
        upWheres.set(upResult);
        return result;
    }

    private Stat getInnerStatRows(StatType type) {
        return getInnerJoin().getInnerStatKeys(type).getRows();
    }
    // корректирует statValue со statKeys
    public Stat getAdjustStatValue(StatType type, Stat valueStat) {
        return getInnerStatRows(type).min(valueStat);
    }
    public abstract InnerJoin<?, ?> getInnerJoin();
    public InnerJoin<?, ?> getBaseJoin() {
        return getInnerJoin();
    }

    protected abstract InnerExpr translate(MapTranslate translator);
}

package lsfusion.server.data.expr.join.select;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.NullableExprInterface;
import lsfusion.server.data.expr.join.base.BaseJoin;
import lsfusion.server.data.expr.join.inner.InnerJoin;
import lsfusion.server.data.expr.join.inner.InnerJoins;
import lsfusion.server.data.expr.join.query.QueryJoin;
import lsfusion.server.data.expr.join.where.WhereJoin;
import lsfusion.server.data.expr.join.where.WhereJoins;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.query.compile.where.AbstractUpWhere;
import lsfusion.server.data.query.compile.where.UpWhere;
import lsfusion.server.data.query.compile.where.UpWheres;
import lsfusion.server.data.stat.*;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.physics.admin.Settings;

public class ExprIndexedJoin extends ExprJoin<ExprIndexedJoin> {

    private final Compare compare;
    private final InnerJoins valueJoins;
    private boolean not; // true только при IS NULL, вообще пока непонятно зачем вообще нужен (по идее для того чтобы разбивать OR с NULL и limit использовал индекс ??? )
    private boolean isOrderTop;

    @Override
    public String toString() {
        return baseExpr + " " + compare + " " + valueJoins + " " + not;
    }

    public ExprIndexedJoin(BaseExpr baseExpr, Compare compare, BaseExpr compareExpr, boolean isOrderTop) {
        this(baseExpr, compare, getInnerJoins(compareExpr), false, isOrderTop);
        assert compareExpr.isValue();
    }
    // IS NULL конструктор
    public ExprIndexedJoin(BaseExpr baseExpr, boolean isOrderTop) {
        this(baseExpr, Compare.LESS, InnerJoins.EMPTY, true, isOrderTop);
    }
    private ExprIndexedJoin(BaseExpr baseExpr, Compare compare, InnerJoins valueJoins, boolean not, boolean isOrderTop) {
        super(baseExpr);
        assert !compare.equals(Compare.EQUALS);
        assert baseExpr.isIndexed();
        this.valueJoins = valueJoins;
        this.compare = compare;
        this.not = not;
        this.isOrderTop = isOrderTop;
    }

    public boolean isOrderTop() {
        return isOrderTop;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat, StatType type, boolean oldMech) {
        if (not)
            return new StatKeys<>(SetFact.EMPTY(), Stat.ONE);
        else {
            if (oldMech) {
                if (compare.equals(Compare.EQUALS) && !givesNoKeys()) { // если не дает ключей, нельзя уменьшать статистику, так как паковка может съесть другие join'ы и тогда будет висячий ключ
                    assert false; // так как !compare.equals(Compare.EQUALS)
                    return new StatKeys<>(SetFact.singleton(0), Stat.ONE);
                } else
                    return new StatKeys<>(SetFact.singleton(0), baseExpr.getTypeStat(keyStat, true));
            }
            throw new UnsupportedOperationException(); // так как вырезается в buildGraph
        }
    }

    @Override
    public Cost getPushedCost(KeyStat keyStat, StatType type, Cost pushCost, Stat pushStat, ImMap<Integer, Stat> pushKeys, ImMap<Integer, Stat> pushNotNullKeys, ImMap<BaseExpr, Stat> pushProps, Result<ImSet<Integer>> rPushedKeys, Result<ImSet<BaseExpr>> rPushedProps) {
        if (not)
            return Cost.ONE;
        throw new UnsupportedOperationException(); // так как вырезается в buildGraph
    }

    //    @Override
//    protected Stat getStat(KeyStat keyStat) {
//        assert !not;
//
//        if (compare.equals(Compare.EQUALS) && !givesNoKeys()) { // если не дает ключей, нельзя уменьшать статистику, так как паковка может съесть другие join'ы и тогда будет висячий ключ
//            assert false; // так как !compare.equals(Compare.EQUALS)
//            return Stat.ONE;
//        } else
//            return baseExpr.getTypeStat(keyStat, true);
//    }

    public int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + compare.hashCode()) + valueJoins.hash(hashContext.values) + 13 + (not ? 1 : 0) + (isOrderTop ? 3 : 0);
    }

    protected ExprIndexedJoin translate(MapTranslate translator) {
        return new ExprIndexedJoin(baseExpr.translateOuter(translator), compare, valueJoins.translate(translator.mapValues()), not, isOrderTop);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && compare.equals(((ExprIndexedJoin)o).compare) && valueJoins.equals(((ExprIndexedJoin)o).valueJoins) && not == ((ExprIndexedJoin)o).not && isOrderTop == ((ExprIndexedJoin)o).isOrderTop;
    }

    @Override
    public ImSet<NullableExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
        if(not)
            return SetFact.EMPTY();
        return super.getExprFollows(includeInnerWithoutNotNull, recursive);
    }

    @Override
    public ImMap<Integer, BaseExpr> getJoins() {
        if(not)
            return MapFact.EMPTY();
        return super.getJoins();
    }

    @Override
    public InnerJoins getInnerJoins() {
        if(not)
            return InnerJoins.EMPTY;
        return super.getInnerJoins().and(valueJoins);
    }

    public boolean givesNoKeys() {
        return not || super.givesNoKeys();
    }

    private enum IntervalType {
        LEFT, RIGHT, FULL;

        public IntervalType and(IntervalType type) {
            if(this == type)
                return this;
            return FULL;
        }

    }
    
    public boolean isNotNull() {
        return !not;
    }

    private static IntervalType getIntervalType(Compare compare) {
        assert !compare.equals(Compare.EQUALS);
        if(compare == Compare.GREATER || compare == Compare.GREATER_EQUALS)
            return IntervalType.LEFT;
        if(compare == Compare.LESS || compare == Compare.LESS_EQUALS)
            return IntervalType.RIGHT;
        if(compare == Compare.CONTAINS || compare == Compare.MATCH)
            return IntervalType.FULL;

        return null;
    }

    // excludeQueryJoin нужен чтобы она не считала его источником ключей => бесконечное проталкивание
    public static ImSet<KeyExpr> getInnerKeys(BaseJoin[] wheres, WhereJoin excludeJoin) {
        MSet<KeyExpr> mInnerKeys = SetFact.mSet();
        for(BaseJoin<?> where : wheres) {
            if (where instanceof InnerJoin && (excludeJoin == null || !BaseUtils.hashEquals(where, excludeJoin))) {
                ImSet<BaseExpr> whereKeys = where.getJoins().values().filterCol(element -> element instanceof KeyExpr).toSet();
                mInnerKeys.addAll(BaseUtils.<ImSet<KeyExpr>>immutableCast(whereKeys));
            }
        }
        return mInnerKeys.immutable();
    }

    public static void fillIntervals(ImSet<ExprIndexedJoin> exprs, MList<WhereJoin> mResult, Result<UpWheres<WhereJoin>> upAdjWheres, WhereJoin[] wheres, QueryJoin excludeQueryJoin) {
        ImMap<BaseExpr, ImSet<ExprIndexedJoin>> exprIndexedJoins = exprs.group(new BaseUtils.Group<BaseExpr, ExprIndexedJoin>() {
            public BaseExpr group(ExprIndexedJoin key) {
                return key.baseExpr;
            }});

        MMap<WhereJoin, UpWhere> mUpIntervalWheres = null;
        if(upAdjWheres != null)
            mUpIntervalWheres = MapFact.mMapMax(exprIndexedJoins.size(), AbstractUpWhere.and());

        int intStat = Settings.get().getAverageIntervalStat();
        ImSet<KeyExpr> innerKeys = null;
        for(int i=0,size=exprIndexedJoins.size();i<size;i++) {
            ImSet<ExprIndexedJoin> joins = exprIndexedJoins.getValue(i);
            BaseExpr expr = exprIndexedJoins.getKey(i);

            boolean fixedInterval = true;
            if(intStat <= 0)
                fixedInterval = false;

            if(fixedInterval) {
                ExprIndexedJoin.IntervalType result = null;
                for (ExprIndexedJoin join : joins) {
                    IntervalType joinType = getIntervalType(join.compare);
                    if (result == null)
                        result = joinType;
                    else
                        result = result.and(joinType);
                }
                fixedInterval = result == IntervalType.FULL;
            }

            if(fixedInterval && expr instanceof KeyExpr) { // по идее эта обработка не нужна, но тогда могут появляться висячие ключи (так как a>=1 AND a<=5 будет убивать другие join'ы), хотя строго говоря потом можно наоборот поддержать эти случаи, тогда a>=1 AND a<=5 будет работать
                if(excludeQueryJoin == null || WhereJoins.givesNoKeys(excludeQueryJoin, (KeyExpr)expr) || Settings.get().isDisableExperimentalFeatures()) {
                    if(innerKeys == null)
                        innerKeys = getInnerKeys(wheres, excludeQueryJoin);
                    if(!innerKeys.contains((KeyExpr)expr)) // висячий ключ
                        fixedInterval = false;
                }
            }

            InnerJoins valueJoins = InnerJoins.EMPTY;
            for(ExprIndexedJoin join : joins)
                valueJoins = valueJoins.and(join.valueJoins);
            WhereJoin adjJoin;
            if(fixedInterval) // assert что все остальные тоже givesNoKeys
                adjJoin = new ExprIntervalJoin(expr, new Stat(intStat, true), valueJoins);
            else
                adjJoin = new ExprStatJoin(expr, Stat.ALOT, valueJoins, false);
            mResult.add(adjJoin);

            if(upAdjWheres != null)
                for(ExprIndexedJoin join : joins)
                    mUpIntervalWheres.add(adjJoin, upAdjWheres.result.get(join));
        }

        if(upAdjWheres != null)
            upAdjWheres.set(new UpWheres<>(upAdjWheres.result.addExcl(mUpIntervalWheres.immutable())));
    }
}

package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.TwinImmutableObject;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.SymmAddValue;
import lsfusion.base.col.interfaces.mutable.add.MAddMap;
import lsfusion.interop.Compare;
import lsfusion.server.caches.hash.HashContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.NotNullExprInterface;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.query.stat.KeyStat;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.query.stat.WhereJoin;
import lsfusion.server.data.translator.MapTranslate;

public class ExprIndexedJoin extends ExprJoin<ExprIndexedJoin> {

    private final Compare compare;
    private final Expr compareExpr;
    private boolean not;
    private boolean isOrderTop;

    @Override
    public String toString() {
        return baseExpr + " " + compare + " " + compareExpr + " " + not;
    }

    public ExprIndexedJoin(BaseExpr baseExpr, Compare compare, Expr compareExpr, boolean not, boolean isOrderTop) {
        super(baseExpr);
        assert !compare.equals(Compare.EQUALS);
        assert compareExpr.isValue();
        assert baseExpr.isIndexed();
        this.compareExpr = compareExpr;
        this.compare = compare;
        this.not = not;
        this.isOrderTop = isOrderTop;
    }

    public boolean isOrderTop() {
        return isOrderTop;
    }

    public StatKeys<Integer> getStatKeys(KeyStat keyStat) {
        if(not)
            return new StatKeys<Integer>(SetFact.<Integer>EMPTY(), Stat.ONE);
        else
            if(compare.equals(Compare.EQUALS) && !givesNoKeys()) // если не дает ключей, нельзя уменьшать статистику, так как паковка может съесть другие join'ы и тогда будет висячий ключ
                return new StatKeys<Integer>(SetFact.singleton(0), Stat.ONE);
            else
                return new StatKeys<Integer>(SetFact.singleton(0), baseExpr.getTypeStat(keyStat, true));
    }

    protected int hash(HashContext hashContext) {
        return 31 * (31 * super.hash(hashContext) + compare.hashCode()) + compareExpr.hashOuter(hashContext) + 13 + (not ? 1 : 0) + (isOrderTop ? 3 : 0);
    }

    protected ExprIndexedJoin translate(MapTranslate translator) {
        return new ExprIndexedJoin(baseExpr.translateOuter(translator), compare, compareExpr, not, isOrderTop);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return super.calcTwins(o) && compare.equals(((ExprIndexedJoin)o).compare) && compareExpr.equals(((ExprIndexedJoin)o).compareExpr) && not == ((ExprIndexedJoin)o).not && isOrderTop == ((ExprIndexedJoin)o).isOrderTop;
    }

    @Override
    public ImSet<NotNullExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive) {
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
        return super.getInnerJoins();
    }

    public boolean givesNoKeys() {
        return not || super.givesNoKeys();
    }


    private enum IntervalType {
        LEFT, RIGHT, FULL
    }

    private static IntervalType getIntervalType(Compare compare) {
        assert !compare.equals(Compare.EQUALS);
        if(compare == Compare.GREATER || compare == Compare.GREATER_EQUALS)
            return IntervalType.LEFT;
        if(compare == Compare.LESS || compare == Compare.LESS_EQUALS)
            return IntervalType.RIGHT;
        if(compare == Compare.LIKE)
            return IntervalType.FULL;

        return null;
    }
    // определяет замыкающиеся диапазоны
    public static ImSet<ExprIndexedJoin> getIntervals(WhereJoin[] wheres) {
        MAddMap<BaseExpr, IntervalType> intervals = MapFact.mAddMap(new SymmAddValue<BaseExpr, IntervalType>() {
            public IntervalType addValue(BaseExpr key, IntervalType prevValue, IntervalType newValue) {
                if(BaseUtils.hashEquals(prevValue, newValue))
                    return prevValue;
                return IntervalType.FULL;
            }
        });
        for(WhereJoin where : wheres) {
            if(where instanceof ExprIndexedJoin) {
                ExprIndexedJoin eiJoin = (ExprIndexedJoin) where;
                if(!eiJoin.givesNoKeys()) { // по идее эта проверка не нужна, но тогда могут появляться висячие ключи, хотя строго говоря потом можно наоборот поддержать эти случаи, тогда a>=1 AND a<=5 будет работать
                    IntervalType type = getIntervalType(eiJoin.compare);
                    if (type != null)
                        intervals.add(eiJoin.baseExpr, type);
                }
            }
        }

        MExclSet<ExprIndexedJoin> mResult = SetFact.mExclSetMax(wheres.length);
        for(WhereJoin where : wheres) {
            if (where instanceof ExprIndexedJoin) {
                ExprIndexedJoin eiJoin = (ExprIndexedJoin) where;
                IntervalType type = intervals.get(eiJoin.baseExpr);
                if(type != null && type == IntervalType.FULL)
                    mResult.exclAdd(eiJoin);
            }
        }
        return mResult.immutable();
    }
}

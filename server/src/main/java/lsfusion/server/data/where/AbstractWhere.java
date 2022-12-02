package lsfusion.server.data.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.base.caches.IdentityQuickLazy;
import lsfusion.server.base.caches.ManualLazy;
import lsfusion.server.base.caches.TwinLazy;
import lsfusion.server.data.AbstractSourceJoin;
import lsfusion.server.data.caches.AbstractOuterContext;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.join.query.GroupExprJoinsWhere;
import lsfusion.server.data.expr.join.query.GroupExprWhereJoins;
import lsfusion.server.data.expr.join.where.GroupJoinsWhere;
import lsfusion.server.data.expr.join.where.GroupSplitWhere;
import lsfusion.server.data.expr.join.where.GroupStatType;
import lsfusion.server.data.expr.join.where.KeyEquals;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.key.ParamExpr;
import lsfusion.server.data.expr.where.NotNullWhere;
import lsfusion.server.data.expr.where.classes.data.CompareWhere;
import lsfusion.server.data.expr.where.classes.data.EqualsWhere;
import lsfusion.server.data.expr.where.pull.ExclPullWheres;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.stat.StatKeys;
import lsfusion.server.data.stat.StatType;
import lsfusion.server.data.table.Table;
import lsfusion.server.data.translate.ExprTranslator;
import lsfusion.server.data.translate.MapTranslate;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.classes.ClassExprWhere;
import lsfusion.server.data.where.classes.MeanClassWheres;

public abstract class AbstractWhere extends AbstractSourceJoin<Where> implements Where {

    public abstract AbstractWhere not();

    public Where and(Where where) {
        return and(where, false); // A AND B = not(notA OR notB)
    }
    public Where and(Where where, boolean packExprs) {
        return not().or(where.not(), packExprs).not(); // A AND B = not(notA OR notB)
    }
    public CheckWhere andCheck(CheckWhere where) {
        return not().orCheck(where.not()).not(); // A AND B = not(notA OR notB)
    }
    public Where or(Where where) {
        return or(where, false);
    }
    public Where or(Where where, boolean packExprs) {
//        assert BaseUtils.hashEquals(OrWhere.oldor(this, where, packExprs),OrWhere.newor(this, where, packExprs));
        return OrWhere.or(this, where, packExprs);
    }
    public CheckWhere orCheck(CheckWhere where) {
        return OrWhere.orCheck(this,where);
    }

    public Where exclOr(Where where) {
//        assert OrWhere.checkTrue(not(), where.not());
        return or(where);
    }

    public Where followFalse(Where falseWhere) {
        return followFalse(falseWhere, false);
    }
    public Where followFalse(Where falseWhere, boolean packExprs) {
        return followFalseChange(falseWhere, packExprs, new FollowChange());
    }
    public Where followFalseChange(Where falseWhere, boolean packExprs, FollowChange change) {
        Where result = followFalse(falseWhere, packExprs, change);
        if(result instanceof ObjectWhere && OrWhere.checkTrue(this,falseWhere)) {
            change.type = FollowType.WIDE;
            result = Where.TRUE();
        }
        return result;
    }

    public <K> ImMap<K, Expr> followTrue(ImMap<K, ? extends Expr> map, final boolean pack) {
        return map.mapValues(value -> value.followFalse(not(), pack));
    }

    public ImList<Expr> followFalse(ImList<Expr> list, final boolean pack) {
        return list.mapListValues((Expr value) -> value.followFalse(AbstractWhere.this, pack));
    }

    public ImSet<Expr> followFalse(ImSet<Expr> set, final boolean pack) {
        return set.mapSetValues(value -> value.followFalse(AbstractWhere.this, pack));
    }

    public <K> ImOrderMap<Expr, K> followFalse(ImOrderMap<Expr, K> map, final boolean pack) {
        return map.mapMergeOrderKeys(value -> value.followFalse(AbstractWhere.this, pack));
    }

    public boolean means(CheckWhere where) {
        return OrWhere.checkTrue(not(),where);
    }

//    public static Where toWhere(IsClassWhere[] wheres) { // чисто оптимизационная вещь для классов
//        return toWhere((AndObjectWhere[])wheres);
//    }

    protected static Where toWhere(AndObjectWhere[] wheres) {
        return toWhere(wheres, false);
    }
    protected static Where toWhere(AndObjectWhere[] wheres, boolean check) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new OrWhere(wheres, check);
    }
    protected static Where toWhere(AndObjectWhere[] wheres, CheckWhere siblingsWhere) {
        if(wheres.length==1)
            return wheres[0];
        else
        if(wheres.length==0)
            return Where.FALSE();
        else
            return new OrWhere(wheres, ((OrWhere)siblingsWhere).check);
    }

    protected static Where toWhere(OrObjectWhere[] wheres) {
        return toWhere(wheres, false);
    }
    protected static Where toWhere(OrObjectWhere[] wheres, boolean check) {
        if(wheres.length==1)
            return wheres[0];
        else
            return new AndWhere(wheres, check);
    }
    protected static Where toWhere(OrObjectWhere[] wheres, CheckWhere siblingsWhere) {
        if(wheres.length==1)
            return wheres[0];
        else
        if(wheres.length==0)
            return Where.TRUE();
        else
            return new AndWhere(wheres, ((AndWhere)siblingsWhere).check);
    }

    protected static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i) {
        AndObjectWhere[] siblings = new AndObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }
    protected static Where siblingsWhere(AndObjectWhere[] wheres, int i) {
        return toWhere(siblings(wheres,i));
    }
    protected static AndObjectWhere[] siblings(AndObjectWhere[] wheres,int i,int numWheres) {
        AndObjectWhere[] siblings = new AndObjectWhere[numWheres-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,numWheres-i-1);
        return siblings;
    }
    protected static OrObjectWhere[] siblings(OrObjectWhere[] wheres,int i) {
        OrObjectWhere[] siblings = new OrObjectWhere[wheres.length-1];
        System.arraycopy(wheres,0,siblings,0,i);
        System.arraycopy(wheres,i+1,siblings,i,wheres.length-i-1);
        return siblings;
    }

    public ClassExprWhere classWhere = null;
    @ManualLazy
    public ClassExprWhere getClassWhere() {
        if(classWhere==null)
            classWhere = calculateClassWhere();
        return classWhere;
    }
    public abstract ClassExprWhere calculateClassWhere();


    private MeanClassWheres meanClassWheres = null;
    private MeanClassWheres noNotsMeanClassWheres = null;
    @ManualLazy
    public MeanClassWheres groupMeanClassWheres(boolean useNots) {
        if(useNots) {
            if(meanClassWheres==null)
                meanClassWheres = calculateMeanClassWheres(useNots);
            return meanClassWheres;
        } else {
            if(noNotsMeanClassWheres==null)
                noNotsMeanClassWheres = calculateMeanClassWheres(useNots);
            return noNotsMeanClassWheres;
        }
    }
    public abstract MeanClassWheres calculateMeanClassWheres(boolean useNots);

    private ImMap<BaseExpr, BaseExpr> getExprValues(boolean and, boolean only) {
        MMap<BaseExpr, BaseExpr> result = MapFact.mMap(MapFact.override());
        for(Where opWhere : and?getOr():getAnd()) {
            BaseExpr expr = null; BaseExpr valueExpr = null;
            if(opWhere instanceof EqualsWhere) {
                CompareWhere where = (CompareWhere)opWhere;
                if(where.operator1.isValue()) { //  && !(where.operator2 instanceof ParamExpr && and))
                    if(!where.operator2.isValue()) { // если operator2 тоже value то нет смысла, обозначать
                        expr = where.operator2;
                        valueExpr = where.operator1;
                    }
                } else
                if(where.operator2.isValue()) { // && !(where.operator1 instanceof ParamExpr && and))
                    expr = where.operator1;
                    valueExpr = where.operator2;
                }
            }
            
            if(expr!=null)
                result.add(expr, valueExpr);
            else
                if(only)
                    return null;
        }
        return result.immutable();
    }

    private ImMap<BaseExpr, BaseExpr> getExprValues(boolean and) {
        return getExprValues(and, false);
    }

    @TwinLazy
    public ImMap<BaseExpr, BaseExpr> getExprValues() {
        return getExprValues(true);
    }

    public ImMap<BaseExpr, BaseExpr> getOnlyExprValues() {
        return getExprValues(true, true);
    }

    @TwinLazy
    public ImMap<BaseExpr, BaseExpr> getNotExprValues() {
        return not().getExprValues(false);
    }

    public Where mapWhere(ImMap<KeyExpr, ? extends Expr> map) {
        return new Query<>(map.keys().toRevMap(), this).join(map).getWhere();
    }

    // так как используется в подзапросах еще, и может быть сложным вычислением, можно было бы хранить чисто в течении компиляции запроса
    @IdentityQuickLazy
    public <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getPackWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop) {
        Pair<ImCol<GroupJoinsWhere>,Boolean> whereJoinsExcl = getWhereJoins(tryExclusive, keepStat, StatType.PACK, orderTop);
        ImCol<GroupJoinsWhere> whereJoins = whereJoinsExcl.first;
        boolean exclusive = whereJoinsExcl.second;

        MCol<GroupJoinsWhere> mResult = ListFact.mColFilter(whereJoins);
        MList<Where> mRecPacks = ListFact.mListMax(whereJoins.size());
        long currentComplexity = hasUnionExpr() ? getComplexity(false) : Long.MAX_VALUE;
        if(!exclusive)
            mRecPacks.add(Where.FALSE());
        for(GroupJoinsWhere innerJoin : whereJoins) {
            if(innerJoin.isComplex())
                mResult.add(innerJoin);
            else { // не будем запускать рекурсию
                Where fullWhere = innerJoin.getFullWhere();
                Where fullPackWhere = fullWhere.pack();
                if(BaseUtils.hashEquals(fullWhere, fullPackWhere))
                    mResult.add(innerJoin);
                else {
                    boolean isProceeded = false;
                    if(!exclusive) {
                        int last = mRecPacks.size() - 1;
                        Where merged = mRecPacks.get(last).or(fullPackWhere);
                        if(merged.getComplexity(false) < currentComplexity) { // prevent infinite recursion, getCommonWhere can get into UnionExpr then reassemble them and the hashEquals check will not work
                            mRecPacks.set(last, merged);
                            isProceeded = true;
                        }
                    }
                    if(!isProceeded) {
                        if (fullPackWhere.getComplexity(false) < currentComplexity) // also can be infinite recursion because of union exprs
                            mRecPacks.add(fullPackWhere);
                        else
                            mResult.add(innerJoin);
                    }
                }
            }
        }
        ImCol<GroupJoinsWhere> result = ListFact.imColFilter(mResult, whereJoins);
        if(result.size()==whereJoins.size()) // optimization
            return whereJoinsExcl;

        ImList<Where> recPacks = mRecPacks.immutableList();
        for(Where recPack : recPacks) {
            Pair<ImCol<GroupJoinsWhere>, Boolean> recWhereJoins = recPack.getPackWhereJoins(exclusive, keepStat, orderTop);
            exclusive = exclusive && recWhereJoins.second;
            result = KeyEquals.merge(result, recWhereJoins.first, orderTop);
        }
        return new Pair<>(result, exclusive);
    }

    // 2-й параметр чисто для оптимизации пока
    public <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, ImSet<K> keepStat, StatType statType, ImOrderSet<Expr> orderTop) {
        return getKeyEquals().getWhereJoins(tryExclusive, keepStat, statType, orderTop);
    }

    public <K extends BaseExpr> ImCol<GroupSplitWhere<K>> getSplitJoins(ImSet<K> keys, StatType statType, boolean exclusive, GroupStatType type) {
        return getKeyEquals().getSplitJoins(exclusive, keys, statType, type);
    }
    
    public <K extends BaseExpr> ImCol<GroupJoinsWhere> getWhereJoins(ImSet<K> keys, StatType statType, boolean groupPackStat) {
        return getKeyEquals().getWhereJoins(keys, statType, groupPackStat);
    }

    public <K extends Expr> ImCol<GroupSplitWhere<K>> getSplitJoins(final boolean exclusive, ImSet<K> exprs, final StatType statType, final GroupStatType type) {
        return new ExclPullWheres<ImCol<GroupSplitWhere<K>>, K, Where>() {
            protected ImCol<GroupSplitWhere<K>> proceedBase(Where data, ImMap<K, BaseExpr> map) {
                return GroupSplitWhere.mapBack(data.and(Expr.getWhere(map)).getSplitJoins(
                        map.values().toSet(), statType, exclusive, type), map);
            }

            protected ImCol<GroupSplitWhere<K>> initEmpty() {
                return SetFact.EMPTY();
            }

            protected ImCol<GroupSplitWhere<K>> add(ImCol<GroupSplitWhere<K>> op1, ImCol<GroupSplitWhere<K>> op2) {
                return type.merge(op1, op2, false);
            }
        }.proceed(this, exprs.toMap());
    }

    public <K extends Expr> GroupExprWhereJoins<K> getGroupExprWhereJoins(ImSet<K> exprs, StatType statType, boolean forcePackReduce) {
        return getGroupExprWhereJoins(exprs.toMap(), statType, forcePackReduce);
    }

    @Override
    public <K extends Expr> GroupExprWhereJoins<K> getGroupExprWhereJoins(ImMap<K, ? extends Expr> exprs, final StatType statType, final boolean forcePackReduce) {
        return new ExclPullWheres<GroupExprWhereJoins<K>, K, Where>() {
            protected GroupExprWhereJoins<K> proceedBase(Where data, ImMap<K, BaseExpr> map) {
                return GroupExprWhereJoins.create(data.and(Expr.getWhere(map)).getWhereJoins(
                        map.values().toSet(), statType, forcePackReduce), map, statType, forcePackReduce);
            }

            protected GroupExprWhereJoins<K> initEmpty() {
                return GroupExprWhereJoins.EMPTY();
            }

            protected GroupExprWhereJoins<K> add(GroupExprWhereJoins<K> op1, GroupExprWhereJoins<K> op2) {
                return op1.merge(op2);
            }
        }.proceed(this, exprs);
    }

    public <K extends Expr> ImCol<GroupExprJoinsWhere<K>> getGroupExprJoinsWheres(ImMap<K, ? extends Expr> exprs, final StatType statType, final boolean forcePackReduce) {
        return new ExclPullWheres<ImCol<GroupExprJoinsWhere<K>>, K, Where>() {
            protected ImCol<GroupExprJoinsWhere<K>> proceedBase(Where data, ImMap<K, BaseExpr> map) {
                return GroupExprJoinsWhere.create(data.and(Expr.getWhere(map)).getWhereJoins(
                        map.values().toSet(), statType, forcePackReduce), map, statType, forcePackReduce);
            }

            protected ImCol<GroupExprJoinsWhere<K>> initEmpty() {
                return SetFact.EMPTY();
            }

            protected ImCol<GroupExprJoinsWhere<K>> add(ImCol<GroupExprJoinsWhere<K>> op1, ImCol<GroupExprJoinsWhere<K>> op2) {
                return op1.mergeCol(op2);
            }
        }.proceed(this, exprs);
    }

    @IdentityLazy
    public <K extends BaseExpr> ImCol<GroupJoinsWhere> getPushedWhereJoins(ImSet<K> keys, StatType statType) {
        return getWhereJoins(keys, statType, false);
    }

    public <K extends BaseExpr, PK extends BaseExpr> StatKeys<K> getPushedStatKeys(final ImSet<K> groups, final StatType type, final StatKeys<PK> pushStatKeys) { // assertion что ключи groups входят в это where
        return StatKeys.or(getPushedWhereJoins(groups, type), value -> value.getStatKeys(groups, type, pushStatKeys), groups);
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(final ImSet<K> groups, final StatType type) { // assertion что ключи groups входят в это where
        assert getOuterKeys().containsAll(AbstractOuterContext.getOuterSetKeys(groups));
        return getPushedStatKeys(groups, type, StatKeys.<KeyExpr>NOPUSH());
    }

    @IdentityLazy
    public <K extends ParamExpr> StatKeys<K> getFullStatKeys(ImSet<K> groups, StatType type) { // assertion что ключи groups являются ключами этого where
        assert BaseUtils.hashEquals(getOuterKeys(), groups);
        return getStatKeys(groups, type);
    }

    public Stat getStatRows(StatType type) {
        return getFullStatKeys(BaseUtils.<ImSet<KeyExpr>>immutableCast(getOuterKeys()), type).getRows();
    }

    public Type getKeyType(ParamExpr expr) {
        return getClassWhere().getKeyType(expr);
    }
    public Stat getKeyStat(ParamExpr key, boolean forJoin) {
        return getClassWhere().getKeyStat(key, forJoin);
    }
    public Where getKeepWhere(KeyExpr expr, boolean noInnerFollows) {
        return getClassWhere().getKeepWhere(expr, noInnerFollows);
    }

    protected abstract Where translate(MapTranslate translator);

    public abstract KeyEquals calculateKeyEquals(); // для аспекта public

    private KeyEquals keyEquals = null;
    @ManualLazy
    public KeyEquals getKeyEquals() {
        if(keyEquals == null)
            keyEquals = calculateKeyEquals();
        return keyEquals;
    }

    public Where xor(Where where) {
        return and(where.not()).or(not().and(where));
    }

    public Where ifElse(Where trueWhere, Where falseWhere) {
        return and(trueWhere).exclOr(falseWhere.and(not()));
    }

    private final static AddValue<Object, Where> addOr = new SymmAddValue<Object, Where>() {
        public Where addValue(Object key, Where prevValue, Where newValue) {
            return prevValue.or(newValue);
        }
    };
    public static <T> AddValue<T, Where> addOr() {
        return (AddValue<T, Where>) addOr;
    }

    private final static AddValue<Object, CheckWhere> addOrCheck = new SymmAddValue<Object, CheckWhere>() {
        public CheckWhere addValue(Object key, CheckWhere prevValue, CheckWhere newValue) {
            return prevValue.orCheck(newValue);
        }
    };
    public static <T> AddValue<T, CheckWhere> addOrCheck() {
        return (AddValue<T, CheckWhere>) addOrCheck;
    }

    @Override
    public boolean needMaterialize() {
        KeyEquals keyEquals = getKeyEquals();
        for(int i=0,size=keyEquals.size();i<size;i++) {
            Where where = keyEquals.getValue(i);
            if(!(where.isTrue() || where instanceof NotNullWhere || where instanceof Table.Join.IsIn))
                return true;
        }

        return super.needMaterialize();
    }

    @Override
    public Where translateExpr(ExprTranslator translator) {
        return super.translateExpr(translator);
    }

    @Override
    public String toString() {
        return getSource(new ToString(getOuterValues()));
    }
}

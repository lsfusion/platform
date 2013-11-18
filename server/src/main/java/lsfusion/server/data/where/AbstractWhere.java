package lsfusion.server.data.where;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.caches.IdentityLazy;
import lsfusion.server.caches.ManualLazy;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.caches.TwinLazy;
import lsfusion.server.data.Table;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.expr.query.Stat;
import lsfusion.server.data.expr.where.NotNullWhere;
import lsfusion.server.data.expr.where.extra.CompareWhere;
import lsfusion.server.data.expr.where.extra.EqualsWhere;
import lsfusion.server.data.expr.where.extra.IsClassWhere;
import lsfusion.server.data.expr.where.pull.ExclPullWheres;
import lsfusion.server.data.query.AbstractSourceJoin;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.innerjoins.GroupJoinsWhere;
import lsfusion.server.data.query.innerjoins.GroupStatType;
import lsfusion.server.data.query.innerjoins.GroupStatWhere;
import lsfusion.server.data.query.innerjoins.KeyEquals;
import lsfusion.server.data.query.stat.StatKeys;
import lsfusion.server.data.translator.MapTranslate;
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
            result = TRUE;
        }
        return result;
    }

    public <K> ImMap<K, Expr> followTrue(ImMap<K, ? extends Expr> map, final boolean pack) {
        return ((ImMap<K, Expr>)map).mapValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.followFalse(not(), pack);
            }
        });
    }

    public ImList<Expr> followFalse(ImList<Expr> list, final boolean pack) {
        return list.mapListValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.followFalse(AbstractWhere.this, pack);
            }});
    }

    public ImSet<Expr> followFalse(ImSet<Expr> set, final boolean pack) {
        return set.mapSetValues(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.followFalse(AbstractWhere.this, pack);
            }});
    }

    public <K> ImOrderMap<Expr, K> followFalse(ImOrderMap<Expr, K> map, final boolean pack) {
        return map.mapMergeOrderKeys(new GetValue<Expr, Expr>() {
            public Expr getMapValue(Expr value) {
                return value.followFalse(AbstractWhere.this, pack);
            }});
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
            return Where.FALSE;
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
            return Where.TRUE;
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
        MMap<BaseExpr, BaseExpr> result = MapFact.mMap(MapFact.<BaseExpr, BaseExpr>override());
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
        return new Query<KeyExpr,Object>(map.keys().toRevMap(),this).join(map).getWhere();
    }

    public <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getPackWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop) {
        Pair<ImCol<GroupJoinsWhere>,Boolean> whereJoinsExcl = getWhereJoins(tryExclusive, keepStat, orderTop);
        ImCol<GroupJoinsWhere> whereJoins = whereJoinsExcl.first;
        boolean exclusive = whereJoinsExcl.second;

        if(whereJoins.size()==1) // нет смысла упаковывать если один whereJoins
            return whereJoinsExcl;
        else {
            MCol<GroupJoinsWhere> mResult = ListFact.mColFilter(whereJoins);
            MList<Where> mRecPacks = ListFact.mListMax(whereJoins.size());
            if(!exclusive) mRecPacks.add(Where.FALSE);
            for(GroupJoinsWhere innerJoin : whereJoins) {
                if(innerJoin.isComplex())
                    mResult.add(innerJoin);
                else { // не будем запускать рекурсию
                    Where fullWhere = innerJoin.getFullWhere();
                    Where fullPackWhere = fullWhere.pack();
                    if(BaseUtils.hashEquals(fullWhere, fullPackWhere))
                        mResult.add(innerJoin);
                    else {
                        if(exclusive)
                            mRecPacks.add(fullPackWhere); // если не exclusive
                        else
                            mRecPacks.set(0, mRecPacks.get(0).or(fullPackWhere));
                    }
                }
            }
            ImCol<GroupJoinsWhere> result = ListFact.imColFilter(mResult, whereJoins);
            ImList<Where> recPacks = mRecPacks.immutableList();

            if(result.size()==whereJoins.size())
                return whereJoinsExcl;

            for(Where recPack : recPacks) {
                Pair<ImCol<GroupJoinsWhere>, Boolean> recWhereJoins = recPack.getPackWhereJoins(exclusive, keepStat, orderTop);
                exclusive = exclusive && recWhereJoins.second;
                result = KeyEquals.merge(result, recWhereJoins.first);
            }
            return new Pair<ImCol<GroupJoinsWhere>, Boolean>(result, exclusive);
        }
    }

    // 2-й параметр чисто для оптимизации пока
    public <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop) {
        return getKeyEquals().getWhereJoins(tryExclusive, keepStat, orderTop);
    }

    public <K extends BaseExpr> ImCol<GroupStatWhere<K>> getStatJoins(ImSet<K> keys, boolean exclusive, GroupStatType type, boolean noWhere) {
        return getKeyEquals().getStatJoins(exclusive, keys, type, noWhere);
    }

    public <K extends Expr> ImCol<GroupStatWhere<K>> getStatJoins(final boolean exclusive, ImSet<K> exprs, final GroupStatType type, final boolean noWhere) {
        return new ExclPullWheres<ImCol<GroupStatWhere<K>>, K, Where>() {
            protected ImCol<GroupStatWhere<K>> proceedBase(Where data, ImMap<K, BaseExpr> map) {
                return GroupStatWhere.mapBack(data.and(Expr.getWhere(map)).getStatJoins(
                        map.values().toSet(), exclusive, type, noWhere), map);
            }

            protected ImCol<GroupStatWhere<K>> initEmpty() {
                return SetFact.EMPTY();
            }

            protected ImCol<GroupStatWhere<K>> add(ImCol<GroupStatWhere<K>> op1, ImCol<GroupStatWhere<K>> op2) {
                return type.merge(op1, op2, noWhere);
            }
        }.proceed(this, exprs.toMap());
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(ImSet<K> groups) { // assertion что ключи groups входят в это where
        StatKeys<K> result = new StatKeys<K>(groups);
        for(GroupStatWhere<K> groupJoin : getStatJoins(groups, false, GroupStatType.ALL, true))
            result = result.or(groupJoin.stats);
        return result;
    }

    @IdentityLazy
    public <K extends ParamExpr> StatKeys<K> getFullStatKeys(ImSet<K> groups) { // assertion что ключи groups являются ключами этого where
        assert BaseUtils.hashEquals(getOuterKeys(), groups);
        return getStatKeys(groups);
    }

    public Stat getStatRows() {
        return getFullStatKeys(BaseUtils.<ImSet<KeyExpr>>immutableCast(getOuterKeys())).rows;
    }

    public <K extends Expr> StatKeys<K> getStatExprs(ImSet<K> groups) {
        StatKeys<K> result = new StatKeys<K>(groups);
        for(GroupStatWhere<K> groupJoin : getStatJoins(false, groups, GroupStatType.ALL, true))
            result = result.or(groupJoin.stats);
        return result;
    }

    public Type getKeyType(ParamExpr expr) {
        return getClassWhere().getKeyType(expr);
    }
    public Stat getKeyStat(ParamExpr key) {
        return getClassWhere().getKeyStat(key);
    }
    public Where getKeepWhere(KeyExpr expr) {
        return getClassWhere().getKeepWhere(expr);
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
}

package platform.server.data.where;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.Pair;
import platform.base.QuickSet;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.TwinLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.pull.ExclPullWheres;
import platform.server.data.query.innerjoins.GroupStatType;
import platform.server.data.query.innerjoins.GroupStatWhere;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.expr.where.extra.CompareWhere;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
import platform.server.data.query.innerjoins.GroupJoinsWhere;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.Type;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.MeanClassWheres;

import java.util.*;

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

    public Where followFalse(Where falseWhere) {
        return followFalse(falseWhere, false);
    }
    public Where followFalse(Where falseWhere, boolean packExprs) {
        Where result = followFalse(falseWhere, packExprs, new FollowChange());
        if(result instanceof ObjectWhere && OrWhere.checkTrue(this,falseWhere))
            result = TRUE;
//        assert BaseUtils.hashEquals(oldff(falseWhere, false, packExprs, new FollowChange()),result);
        return result;
    }

    public <K> Map<K, Expr> followTrue(Map<K, ? extends Expr> map, boolean pack) {
        Map<K, Expr> result = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            result.put(entry.getKey(),entry.getValue().followFalse(not(), pack));
        return result;
    }

    public List<Expr> followFalse(List<Expr> list, boolean pack) {
        List<Expr> result = new ArrayList<Expr>();
        for(Expr item : list)
            result.add(item.followFalse(this, pack));
        return result;
    }

    public <K> OrderedMap<Expr, K> followFalse(OrderedMap<Expr, K> map, boolean pack) {
        OrderedMap<Expr, K> result = new OrderedMap<Expr, K>();
        for(Map.Entry<Expr, K> entry : map.entrySet())
            result.put(entry.getKey().followFalse(this, pack),entry.getValue());
        return result;
    }

    public boolean means(CheckWhere where) {
        return OrWhere.checkTrue(not(),where);
    }

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
        if(wheres.length==1)
            return wheres[0];
        else
            return new AndWhere(wheres, false);
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

    private Map<BaseExpr, BaseExpr> getExprValues(boolean and, boolean only) {
        Map<BaseExpr, BaseExpr> result = new HashMap<BaseExpr, BaseExpr>();
        for(Where opWhere : and?getOr():getAnd()) {
            BaseExpr expr = null; BaseExpr valueExpr = null;
            if(opWhere instanceof EqualsWhere) {
                CompareWhere where = (CompareWhere)opWhere;
                if(where.operator1.isValue()) { //  && !(where.operator2 instanceof KeyExpr && and))
                    if(!where.operator2.isValue()) { // если operator2 тоже value то нет смысла, обозначать
                        expr = where.operator2;
                        valueExpr = where.operator1;
                    }
                } else
                if(where.operator2.isValue()) { // && !(where.operator1 instanceof KeyExpr && and))
                    expr = where.operator1;
                    valueExpr = where.operator2;
                }
            }
            
            if(expr!=null)
                result.put(expr, valueExpr);
            else
                if(only)
                    return null;
        }
        return result;
    }

    private Map<BaseExpr, BaseExpr> getExprValues(boolean and) {
        return getExprValues(and, false);
    }

    @TwinLazy
    public Map<BaseExpr, BaseExpr> getExprValues() {
        return getExprValues(true);
    }

    public Map<BaseExpr, BaseExpr> getOnlyExprValues() {
        return getExprValues(true, true);
    }

    @TwinLazy
    public Map<BaseExpr, BaseExpr> getNotExprValues() {
        return not().getExprValues(false);
    }

    public Where map(Map<KeyExpr, ? extends Expr> map) {
        return new Query<KeyExpr,Object>(BaseUtils.toMap(map.keySet()),this).join(map).getWhere();
    }

    public <K extends BaseExpr> Pair<Collection<GroupJoinsWhere>, Boolean> getPackWhereJoins(boolean tryExclusive, QuickSet<K> keepStat, List<Expr> orderTop) {
        Pair<Collection<GroupJoinsWhere>,Boolean> whereJoinsExcl = getWhereJoins(tryExclusive, keepStat, orderTop);
        Collection<GroupJoinsWhere> whereJoins = whereJoinsExcl.first;
        boolean exclusive = whereJoinsExcl.second;

        if(whereJoins.size()==1) // нет смысла упаковывать если один whereJoins
            return whereJoinsExcl;
        else {
            Collection<GroupJoinsWhere> result = new ArrayList<GroupJoinsWhere>();

            List<Where> recPacks = new ArrayList<Where>();
            if(!exclusive) recPacks.add(Where.FALSE);
            for(GroupJoinsWhere innerJoin : whereJoins) {
                if(innerJoin.isComplex())
                    result.add(innerJoin);
                else { // не будем запускать рекурсию
                    Where packWhere = innerJoin.where.pack();
                    if(BaseUtils.hashEquals(innerJoin.where, packWhere))
                        result.add(innerJoin);
                    else {
                        Where fullPackWhere = innerJoin.keyEqual.getWhere().and(packWhere);
                        if(exclusive)
                            recPacks.add(fullPackWhere); // если не exclusive
                        else
                            recPacks.set(0, recPacks.get(0).or(fullPackWhere));
                    }
                }
            }
            
            if(result.size()==whereJoins.size())
                return whereJoinsExcl;

            for(Where recPack : recPacks) {
                Pair<Collection<GroupJoinsWhere>, Boolean> recWhereJoins = recPack.getPackWhereJoins(exclusive, keepStat, orderTop);
                exclusive = exclusive && recWhereJoins.second;
                result = KeyEquals.merge(result, recWhereJoins.first);
            }
            return new Pair<Collection<GroupJoinsWhere>, Boolean>(result, exclusive);
        }
    }

    // 2-й параметр чисто для оптимизации пока
    public <K extends BaseExpr> Pair<Collection<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, QuickSet<K> keepStat, List<Expr> orderTop) {
        return getKeyEquals().getWhereJoins(tryExclusive, keepStat, orderTop);
    }

    public <K extends BaseExpr> Collection<GroupStatWhere<K>> getStatJoins(QuickSet<K> keys, boolean exclusive, GroupStatType type, boolean noWhere) {
        return getKeyEquals().getStatJoins(exclusive, keys, type, noWhere);
    }

    public <K extends Expr> Collection<GroupStatWhere<K>> getStatJoins(final boolean exclusive, QuickSet<K> exprs, final GroupStatType type, final boolean noWhere) {
        return new ExclPullWheres<Collection<GroupStatWhere<K>>, K, Where>() {
            protected Collection<GroupStatWhere<K>> proceedBase(Where data, Map<K, BaseExpr> map) {
                return GroupStatWhere.mapBack(data.and(Expr.getWhere(map)).getStatJoins(
                        new QuickSet<BaseExpr>(map.values()), exclusive, type, noWhere), map);
            }

            protected Collection<GroupStatWhere<K>> initEmpty() {
                return new ArrayList<GroupStatWhere<K>>();
            }

            protected Collection<GroupStatWhere<K>> add(Collection<GroupStatWhere<K>> op1, Collection<GroupStatWhere<K>> op2) {
                return type.merge(op1, op2, noWhere);
            }
        }.proceed(this, exprs.toMap());
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(QuickSet<K> groups) { // assertion что where keys входят в это where
        StatKeys<K> result = new StatKeys<K>(groups);
        for(GroupStatWhere<K> groupJoin : getStatJoins(groups, false, GroupStatType.ALL, true))
            result = result.or(groupJoin.stats);
        return result;
    }

    public <K extends BaseExpr> StatKeys<K> getStatKeys(Collection<K> groups) { // assertion что where keys входят в это where
        return getStatKeys(new QuickSet<K>(groups));
    }

    @IdentityLazy
    public Stat getStatRows() {
        return getStatKeys(getOuterKeys()).rows;
    }

    public <K extends Expr> StatKeys<K> getStatExprs(QuickSet<K> groups) {
        StatKeys<K> result = new StatKeys<K>(groups);
        for(GroupStatWhere<K> groupJoin : getStatJoins(false, groups, GroupStatType.ALL, true))
            result = result.or(groupJoin.stats);
        return result;
    }

    public Type getKeyType(KeyExpr expr) {
        return getClassWhere().getKeyType(expr);
    }
    public Stat getKeyStat(KeyExpr key) {
        return getClassWhere().getKeyStat(key);
    }
    public Where getKeepWhere(KeyExpr expr) {
        return getClassWhere().getKeepWhere(expr);
    }

    protected abstract Where translate(MapTranslate translator);

    protected abstract KeyEquals calculateKeyEquals();

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
        return and(trueWhere).or(falseWhere.and(not()));
    }
}

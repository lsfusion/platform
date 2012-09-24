package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.expr.where.extra.GreaterWhere;
import platform.server.data.expr.where.extra.InArrayWhere;
import platform.server.data.expr.where.extra.LikeWhere;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.ClassReader;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.*;


public abstract class BaseExpr extends Expr {

    public static Expr create(BaseExpr expr) {
        if(expr.getWhere().getClassWhere().isFalse())
            return NULL;
        else
            return expr;
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    public NotNullExprSet getExprFollows(boolean includeThis, boolean recursive) {
        assert includeThis || recursive; // также предполагается что NotNullExpr includeThis отработал
        return getExprFollows(recursive);
    }
    private NotNullExprSet exprFollows = null;
    @ManualLazy
    public NotNullExprSet getExprFollows(boolean recursive) {
        if(exprFollows==null)
            exprFollows = getBaseJoin().getExprFollows(recursive);
        return exprFollows;
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }

    public ClassReader getReader(KeyType keyType) {
        return getType(keyType); // assert'ится что не null
    }

    protected abstract BaseExpr translate(MapTranslate translator);
    public BaseExpr translateOuter(MapTranslate translator) {
        return (BaseExpr) aspectTranslate(translator);
    }

    public abstract void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere);

    public Expr followFalse(Where where, boolean pack) {
        if(getWhere().means(where))
            return NULL;
        else
            if(pack)
                return packFollowFalse(where);
            else
                return this;
    }

    // для linear'ов делает followFalse, известно что не means(where)
    public Expr packFollowFalse(Where where) {
        return this;
    }

    public abstract ClassExprWhere getClassWhere(AndClassSet classes);

    public Where compareBase(final BaseExpr expr, Compare compareBack) {
        switch(compareBack) {
            case EQUALS:
                return EqualsWhere.create(expr, this);
            case GREATER:
                return GreaterWhere.create(expr, this, false);
            case GREATER_EQUALS:
                if(Settings.instance.isUseGreaterEquals())
                    return GreaterWhere.create(expr, this, true);
                else
                    return GreaterWhere.create(expr, this, false).or(EqualsWhere.create(expr, this));
            case LESS:
                return GreaterWhere.create(this, expr, false);
            case LESS_EQUALS:
                if(Settings.instance.isUseGreaterEquals())
                    return GreaterWhere.create(this, expr, true);
                else
                    return GreaterWhere.create(this, expr, false).or(EqualsWhere.create(expr, this));
            case NOT_EQUALS: // оба заданы и не равно
                return getWhere().and(expr.getWhere()).and(EqualsWhere.create(expr, this).not());
            case START_WITH:
                return LikeWhere.create(expr, this, true);
            case LIKE:
                return LikeWhere.create(expr, this, false);
            case INARRAY:
                return InArrayWhere.create(expr, this);
        }
        throw new RuntimeException("should not be");
    }
    public Where compare(final Expr expr, final Compare compare) {
        return expr.compareBase(this, compare);
    }

    public boolean hasKey(KeyExpr key) {
        return getOuterKeys().contains(key);
    }

    // может возвращать null, оба метода для ClassExprWhere
    public abstract AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and);
    public abstract boolean addAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and, AndClassSet add);

    public static <K> Map<K, Expr> packFollowFalse(Map<K, BaseExpr> mapExprs, Where falseWhere) {
        Map<K, Expr> result = new HashMap<K, Expr>();
        for(Map.Entry<K,BaseExpr> groupExpr : mapExprs.entrySet()) {
            CheckWhere siblingWhere = Where.TRUE;
            for(Map.Entry<K,BaseExpr> sibling : mapExprs.entrySet())
                if(!BaseUtils.hashEquals(sibling.getKey(), groupExpr.getKey())) {
                    Expr siblingExpr = result.get(sibling.getKey());
                    if(siblingExpr==null) siblingExpr = sibling.getValue();
                    siblingWhere = siblingWhere.andCheck(siblingExpr.getWhere());
                }
            result.put(groupExpr.getKey(), groupExpr.getValue().packFollowFalse((Where) falseWhere.orCheck(siblingWhere.not())));
        }
        return result;
    }

    private static <K> Map<K, BaseExpr> pushValues(Map<K, BaseExpr> group, Where falseWhere) {
        Map<BaseExpr, BaseExpr> exprValues = falseWhere.not().getExprValues();
        Map<K, BaseExpr> pushedGroup = new HashMap<K, BaseExpr>();
        for(Map.Entry<K, BaseExpr> groupExpr : group.entrySet()) { // проталкиваем values внутрь
            BaseExpr pushValue = exprValues.get(groupExpr.getValue());
            pushedGroup.put(groupExpr.getKey(), pushValue!=null?pushValue: groupExpr.getValue());
        }
        return pushedGroup;
    }

    public static <K> Map<K, Expr> packPushFollowFalse(Map<K, BaseExpr> mapExprs, Where falseWhere) {
        return packFollowFalse(pushValues(mapExprs, falseWhere), falseWhere);
    }

    public Where getBaseWhere() {
        return Where.TRUE;
    }

    public int getWhereDepth() {
        return 1;
    }

    public abstract Stat getStatValue(KeyStat keyStat);
    public abstract InnerBaseJoin<?> getBaseJoin();

    public QuickSet<OuterContext> calculateOuterDepends() {
        return new QuickSet<OuterContext>(getUsed());
    }

    public abstract Stat getTypeStat(KeyStat keyStat);

    public Stat getTypeStat(Where fullWhere) {
        return getTypeStat((KeyStat) fullWhere);
    }

    public Set<BaseExpr> getBaseExprs() {
        return Collections.singleton(this);
    }

    public Collection<BaseExpr> getUsed() {
        return getBaseJoin().getJoins().values();
    }

    public Where calculateWhere() {
        return getOrWhere().and(getNotNullWhere());
    }
    
    private Where orWhere;
    @ManualLazy
    public Where getOrWhere() {
        if(orWhere==null)
            orWhere = calculateOrWhere();
        return orWhere;
    } 
    
    public Where calculateOrWhere() {
        Where result = Where.TRUE;
        for(BaseExpr baseExpr : getUsed())
            result = result.and(baseExpr.getOrWhere());
        return result;
    }
    
    private Where notNullWhere;
    @ManualLazy
    public Where getNotNullWhere() {
        if(notNullWhere==null)
            notNullWhere = calculateNotNullWhere();
        return notNullWhere;
    }

    public Where calculateNotNullWhere() {
        return getNotNullWhere(getUsed());
    }

    public boolean hasNotNull() {
        return !getNotNullWhere().isTrue();
    }
    
    public static Where getNotNullWhere(Collection<? extends BaseExpr> exprs) {
        Where result = Where.TRUE;
        for(BaseExpr baseExpr : exprs)
            result = result.and(baseExpr.getNotNullWhere());
        return result;
    }

    public boolean isTableIndexed() {
        return false;
    }
}

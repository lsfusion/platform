package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MMap;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.base.col.interfaces.mutable.mapvalue.ImValueMap;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.caches.ManualLazy;
import platform.server.caches.OuterContext;
import platform.server.caches.ParamExpr;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.query.PropStat;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.ExprCase;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.expr.where.extra.GreaterWhere;
import platform.server.data.expr.where.extra.InArrayWhere;
import platform.server.data.expr.where.extra.LikeWhere;
import platform.server.data.query.JoinData;
import platform.server.data.query.stat.BaseJoin;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.ClassReader;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.Collections;
import java.util.Set;


public abstract class BaseExpr extends Expr {

    public static Expr create(BaseExpr expr) {
        if(expr.getWhere().getClassWhere().isFalse())
            return NULL;
        else
            return expr;
    }

    // получает список ExprCase'ов
    public ExprCaseList getCases() {
        return new ExprCaseList(SetFact.<ExprCase>singleton(new ExprCase(Where.TRUE, this)));
    }

    public ImSet<NotNullExpr> getExprFollows(boolean includeThis, boolean recursive) {
        assert includeThis || recursive; // также предполагается что NotNullExpr includeThis отработал
        return getExprFollows(recursive);
    }
    private ImSet<NotNullExpr> exprFollows = null;
    @ManualLazy
    public ImSet<NotNullExpr> getExprFollows(boolean recursive) {
        if(exprFollows==null)
            exprFollows = getBaseJoin().getExprFollows(recursive);
        return exprFollows;
    }

    public void fillJoinWheres(MMap<JoinData, Where> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }

    public ClassReader getReader(KeyType keyType) {
        return getType(keyType); // assert'ится что не null
    }

    protected abstract BaseExpr translate(MapTranslate translator);
    public BaseExpr translateOuter(MapTranslate translator) {
        return (BaseExpr) aspectTranslate(translator);
    }

    public abstract void fillAndJoinWheres(MMap<JoinData, Where> joins, Where andWhere);

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
                if(Settings.get().isUseGreaterEquals())
                    return GreaterWhere.create(expr, this, true);
                else
                    return GreaterWhere.create(expr, this, false).or(EqualsWhere.create(expr, this));
            case LESS:
                return GreaterWhere.create(this, expr, false);
            case LESS_EQUALS:
                if(Settings.get().isUseGreaterEquals())
                    return GreaterWhere.create(this, expr, true);
                else
                    return GreaterWhere.create(this, expr, false).or(EqualsWhere.create(expr, this));
            case NOT_EQUALS: // оба заданы и не равно
                return getWhere().and(expr.getWhere()).and(EqualsWhere.create(expr, this).not());
            case START_WITH:
                return LikeWhere.create(expr, this, true);
            case LIKE:
                return LikeWhere.create(expr, this, null);
            case INARRAY:
                return InArrayWhere.create(expr, this);
            case CONTAINS:
                return LikeWhere.create(expr, this, false);
        }
        throw new RuntimeException("should not be");
    }
    public Where compare(final Expr expr, final Compare compare) {
        return expr.compareBase(this, compare);
    }

    public boolean hasKey(ParamExpr key) {
        return getOuterKeys().contains(key);
    }

    // может возвращать null, оба метода для ClassExprWhere
    public abstract AndClassSet getAndClassSet(ImMap<VariableSingleClassExpr, AndClassSet> and);
    public abstract boolean addAndClassSet(MMap<VariableSingleClassExpr, AndClassSet> and, AndClassSet add);

    public static <K> ImMap<K, Expr> packFollowFalse(final ImMap<K, BaseExpr> mapExprs, Where falseWhere) {

        ImValueMap<K, Expr> result = mapExprs.mapItValues(); // идет обращение к предыдущим значениям
        for(int i=0,size=mapExprs.size();i<size;i++) {
            Where siblingWhere = Where.TRUE;
            for(int j=0;j<size;j++) {
                if(j!=i) {
                    Expr siblingExpr = result.getMapValue(j);
                    if(siblingExpr==null) siblingExpr = mapExprs.getValue(j);
                    siblingWhere = andExprCheck(siblingWhere, siblingExpr.getWhere());
                }
            }
            result.mapValue(i, mapExprs.getValue(i).packFollowFalse(orExprCheck(falseWhere, siblingWhere.not())));
        }
        return result.immutableValue();
    }

    private static <K> ImMap<K, BaseExpr> pushValues(ImMap<K, BaseExpr> group, Where falseWhere) {
        final ImMap<BaseExpr, BaseExpr> exprValues = falseWhere.not().getExprValues();
        return group.mapValues(new GetValue<BaseExpr, BaseExpr>() {
            public BaseExpr getMapValue(BaseExpr value) {
                return BaseUtils.nvl(exprValues.get(value), value);
            }});
    }

    public static <K> ImMap<K, Expr> packPushFollowFalse(ImMap<K, BaseExpr> mapExprs, Where falseWhere) {
        return packFollowFalse(pushValues(mapExprs, falseWhere), falseWhere);
    }

    public Where getBaseWhere() {
        return Where.TRUE;
    }

    public int getWhereDepth() {
        return 1;
    }

    public abstract PropStat getStatValue(KeyStat keyStat);
    public abstract InnerBaseJoin<?> getBaseJoin();

    public ImSet<OuterContext> calculateOuterDepends() {
        return BaseUtils.immutableCast(getUsed().toSet());
    }

    public abstract Stat getTypeStat(KeyStat keyStat);

    public Stat getTypeStat(Where fullWhere) {
        return getTypeStat((KeyStat) fullWhere);
    }

    public Set<BaseExpr> getBaseExprs() {
        return Collections.singleton(this);
    }

    public ImCol<BaseExpr> getUsed() {
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
    
    public static Where getOrWhere(BaseJoin<?> join){
        Where result = Where.TRUE;
        for(BaseExpr baseExpr : join.getJoins().valueIt())
            result = result.and(baseExpr.getOrWhere());
        return result;
    } 
    public Where calculateOrWhere() {
        return getOrWhere(getBaseJoin());
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
    
    public static Where getNotNullWhere(ImCol<? extends BaseExpr> exprs) {
        Where result = Where.TRUE;
        for(BaseExpr baseExpr : exprs)
            result = result.and(baseExpr.getNotNullWhere());
        return result;
    }

    public boolean isTableIndexed() {
        return false;
    }
    
    public boolean compatibleEquals(BaseExpr expr) {
        return BaseUtils.hashEquals(this, expr);
    }
}

package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.QuickMap;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.expr.where.extra.GreaterWhere;
import platform.server.data.expr.where.extra.LikeWhere;
import platform.server.data.where.MapWhere;
import platform.server.data.query.JoinData;
import platform.server.data.translator.MapTranslate;
import platform.server.data.type.ClassReader;
import platform.server.data.where.CheckWhere;
import platform.server.data.where.Where;
import platform.server.data.where.classes.ClassExprWhere;

import java.util.HashMap;
import java.util.Map;


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

    protected abstract VariableExprSet calculateExprFollows();
    private VariableExprSet exprFollows = null;
    @ManualLazy
    public VariableExprSet getExprFollows() {
        if(exprFollows==null)
            exprFollows = calculateExprFollows();
        return exprFollows;
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        fillAndJoinWheres(joins, andWhere.and(getWhere()));
    }

    public ClassReader getReader(KeyType keyType) {
        return getType(keyType); // assert'ится что не null
    }

    public abstract BaseExpr translateOuter(MapTranslate translator);

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
                return GreaterWhere.create(expr, this);
            case GREATER_EQUALS:
                return GreaterWhere.create(expr, this).or(EqualsWhere.create(expr, this));
            case LESS:
                return GreaterWhere.create(this, expr);
            case LESS_EQUALS:
                return GreaterWhere.create(this, expr).or(EqualsWhere.create(expr, this));
            case NOT_EQUALS: // оба заданы и не равно
                return getWhere().and(expr.getWhere()).and(EqualsWhere.create(expr, this).not());
            case START_WITH:
                return LikeWhere.create(expr, this, true);
            case LIKE:
                return LikeWhere.create(expr, this, false);
        }
        throw new RuntimeException("should not be");
    }
    public Where compare(final Expr expr, final Compare compare) {
        return expr.compareBase(this, compare);
    }

    public boolean hasKey(KeyExpr key) {
        return enumKeys(this).contains(key);
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
    public BaseExpr getBaseExpr() {
        return this;
    }

    public int getWhereDepth() {
        return 1;
    }
}

package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.caches.PackInterface;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.IntegralClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.QueryEnvironment;
import platform.server.data.SQLSession;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.cases.ExprCaseList;
import platform.server.data.expr.where.CaseExprInterface;
import platform.server.data.expr.where.ifs.NullExpr;
import platform.server.data.expr.where.ifs.IfCases;
import platform.server.data.expr.where.ifs.IfExpr;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.ExprEnumerator;
import platform.server.data.query.Query;
import platform.server.data.query.SourceJoin;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ClassReader;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;

import java.sql.SQLException;
import java.util.*;

// абстрактный класс выражений

abstract public class Expr extends AbstractSourceJoin<Expr> {

    public static final boolean useCases = false;
    public static final Expr NULL = useCases?new CaseExpr(new ExprCaseList()):NullExpr.instance;
    public static CaseExprInterface newCases() {
        if(useCases)
            return new ExprCaseList();
        else
            return new IfCases();
    }

    public abstract Type getType(KeyType keyType);
    @IdentityLazy
    public Type getSelfType() {
        return getType(getWhere());
    }   

    public abstract ClassReader getReader(KeyType keyType);

    public abstract int getWhereDepth();

    // возвращает Where на notNull
    private Where where=null;
    @ManualLazy
    public Where getWhere() {
        if(where==null)
            where = calculateWhere();
        return where;
    }
    public abstract Where calculateWhere();

    // получает список ExprCase'ов
    public abstract ExprCaseList getCases();

    public abstract Expr classExpr(BaseClass baseClass);

    public abstract Where isClass(AndClassSet set);

    public abstract Where compareBase(BaseExpr expr, Compare compareBack);
    public abstract Where compare(Expr expr, Compare compare);
    public Where compare(Expr expr, boolean min) {
        return compare(expr, Compare.get(min));
    }

    public Where compare(DataObject data, Compare compare) {
        return compare(data.getExpr(),compare);
    }

//    public abstract Expr scale(int coeff);

//    public abstract Expr sum(Expr expr);
    public Expr scale(int coeff) {
        if(coeff==1) return this;

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,coeff);
        return map.getExpr();
    }
    
    public Expr mult(Expr expr, IntegralClass intClass) {
        return FormulaExpr.create2(FormulaExpr.MULT2, intClass, this, expr);
    }

    public Expr sum(Expr expr) {
        // нельзя делать эту оптимизацию так как идет проверка на 0 в логике
//        if(getWhere().means(expr.getWhere().not())) // если не пересекаются то возвращаем case
//            return nvl(expr);

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return map.getExpr();
    }

    public Expr diff(Expr expr) {
        return sum(expr.scale(-1));
    }

    public Expr and(Where where) {
        if(getWhere().means(where))
            return this;

        return ifElse(where, Expr.NULL);
    }
    public Expr ifElse(Where where, Expr elseExpr) {
        if(Expr.useCases)
            return new ExprCaseList(where,this,elseExpr).getFinal();
        else
            return IfExpr.create(where, this, elseExpr);
    }
    public Expr compareExpr(Expr expr, boolean min) {
        return ifElse(compare(expr, min).or(expr.getWhere().not()),expr);
    }
    public Expr max(Expr expr) {
        return compareExpr(expr, false);
    }
    public Expr min(Expr expr) {
        return compareExpr(expr, true);
    }
    public Expr nvl(Expr expr) {
        return ifElse(getWhere(),expr);
    }

    public abstract Expr translateQuery(QueryTranslator translator);

    public static Where getWhere(Iterable<? extends Expr> col) {
        Where where = Where.TRUE;
        for(Expr expr : col)
            where = where.and(expr.getWhere());
        return where;
    }

    public static Where getOrWhere(Iterable<? extends Expr> col) {
        Where where = Where.FALSE;
        for(Expr expr : col)
            where = where.or(expr.getWhere());
        return where;
    }

    public static <K> Where getWhere(Map<K, ? extends Expr> map) {
        return getWhere(map.values());
    }

    public void checkInfiniteKeys() {
        QuickSet<KeyExpr> keys = getOuterKeys();

        Map<KeyExpr,BaseExpr> keyValues = new HashMap<KeyExpr, BaseExpr>();
        Set<KeyExpr> keyRest = new HashSet<KeyExpr>();
        for(KeyExpr key : keys) {
            Type type = key.getType(getWhere());
            if(type instanceof DataClass)
                keyValues.put(key, ((DataClass)type).getDefaultExpr());
            else
                keyRest.add(key);
        }

        Query<KeyExpr,Object> query = new Query<KeyExpr,Object>(BaseUtils.toMap(keyRest));
        query.and(translateQuery(new PartialQueryTranslator(keyValues)).getWhere());
        query.compile(BusinessLogics.debugSyntax);
    }

    // проверка на статичность, временно потом более сложный алгоритм надо будет
    public boolean isValue() {
        return getOuterKeys().isEmpty();
    }
    public static <K> Map<K, ObjectValue> readValues(SQLSession session, BaseClass baseClass, Map<K,Expr> mapExprs, QueryEnvironment env) throws SQLException { // assert что в mapExprs только values
        Map<K, ObjectValue> mapValues = new HashMap<K, ObjectValue>();
        Map<K, Expr> mapExprValues = new HashMap<K, Expr>();
        for(Map.Entry<K, Expr> mapExpr : mapExprs.entrySet())
            if(mapExpr.getValue() instanceof ValueExpr)
                mapValues.put(mapExpr.getKey(), ((ValueExpr) mapExpr.getValue()).getDataObject());
            else if(mapExpr.getValue().getWhere().isFalse())
                mapValues.put(mapExpr.getKey(), NullValue.instance);
            else
                mapExprValues.put(mapExpr.getKey(), mapExpr.getValue());
        if(mapExprValues.isEmpty()) // чисто для оптимизации чтобы лишний раз executeClasses не вызывать
            return mapValues;
        else
            return BaseUtils.merge(mapValues, new Query<Object, K>(new HashMap<Object, KeyExpr>(), mapExprValues, Where.TRUE).executeClasses(session, env, baseClass).singleValue());
    }

    public abstract Where getBaseWhere();

    public abstract Stat getTypeStat(Where fullWhere);

    public abstract Set<BaseExpr> getBaseExprs();
    
    public ObjectValue getObjectValue() {
        return null;
    }
}


package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.ManualLazy;
import platform.server.classes.BaseClass;
import platform.server.classes.DataClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.Query;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.ClassReader;
import platform.server.data.type.Type;
import platform.server.data.where.Where;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;

import java.util.*;

// абстрактный класс выражений

abstract public class Expr extends AbstractSourceJoin<Expr> {

    public static final CaseExpr NULL = new CaseExpr(new ExprCaseList());

    public abstract Type getType(KeyType keyType);
    @IdentityLazy
    public Type getSelfType() {
        return getType(getWhere());
    }   

    public abstract ClassReader getReader(KeyType keyType);

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

    // упрощаем зная where == false
    public abstract Expr followFalse(Where where, boolean pack);
    public Expr pack() {
        return followFalse(Where.FALSE, true);
    }

    public abstract Expr classExpr(BaseClass baseClass);

    public abstract Where isClass(AndClassSet set);

    public abstract Where compare(Expr expr, Compare compare);

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

    public Expr sum(Expr expr) {
        if(getWhere().means(expr.getWhere().not())) // если не пересекаются то возвращаем case
            return nvl(expr);

        LinearOperandMap map = new LinearOperandMap();
        map.add(this,1);
        map.add(expr,1);
        return map.getExpr();
    }

    public Expr and(Where where) {
        if(getWhere().means(where))
            return this;
        return new ExprCaseList(where,this).getExpr();
    }
    public Expr ifElse(Where where, Expr elseExpr) {
        return new ExprCaseList(where,this,elseExpr).getExpr();
    }
    public Expr max(Expr expr) {
        return ifElse(compare(expr, Compare.GREATER).or(expr.getWhere().not()),expr);
    }
    public Expr nvl(Expr expr) {
        return ifElse(getWhere(),expr);
    }

    public abstract Expr translateQuery(QueryTranslator translator);

    public static Where getWhere(Collection<? extends Expr> col) {
        Where where = Where.TRUE;
        for(Expr expr : col)
            where = where.and(expr.getWhere());
        return where;

    }

    public static <K> Where getWhere(Map<K, ? extends Expr> map) {
        return getWhere(map.values());
    }

    public void checkInfiniteKeys() {
        Set<KeyExpr> keys = new HashSet<KeyExpr>();
        enumKeys(keys);

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
        return enumKeys(this).isEmpty();
    }

    public VariableExprSet getExprFollows() {
        return getCases().getExprFollows();
    }
}


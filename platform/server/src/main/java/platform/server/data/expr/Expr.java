package platform.server.data.expr;

import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.caches.Lazy;
import platform.server.classes.BaseClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Reader;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

import net.jcip.annotations.Immutable;

// абстрактный класс выражений

@Immutable
abstract public class Expr extends AbstractSourceJoin<Expr> {

    public static final CaseExpr NULL = new CaseExpr(new ExprCaseList());

    public abstract Type getType(Where where);
    @Lazy
    public Type getSelfType() {
        return getType(getWhere());
    }   

    public abstract Reader getReader(Where where);

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

    public abstract Expr followFalse(Where where);

    public abstract Expr classExpr(BaseClass baseClass);

    public abstract Where isClass(AndClassSet set);

    public abstract Where compare(Expr expr, Compare compare);

    public Where compare(DataObject data, Compare compare) {
        return compare(data.getExpr(),compare);
    }

    public abstract Expr scale(int coeff);

    public abstract Expr sum(Expr expr);

    public Expr and(Where where) {
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
}


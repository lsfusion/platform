package platform.server.data.expr;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.classes.BaseClass;
import platform.server.classes.ConcreteValueClass;
import platform.server.classes.sets.AndClassSet;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.expr.cases.ExprCase;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Reader;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.data.where.Where;

import java.util.Collection;
import java.util.Map;

// абстрактный класс выражений

abstract public class Expr extends AbstractSourceJoin {
    
    public static final CaseExpr NULL = new CaseExpr(new ExprCaseList());

    public abstract Type getType(Where where);
    public abstract Reader getReader(Where where);

    // возвращает Where на notNull
    private Where<?> where=null;
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

    public static Expr formula(String formula, ConcreteValueClass value,Map<String,? extends Expr> params) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<String> mapCase : CaseExpr.pullCases(params))
            result.add(mapCase.where,FormulaExpr.create(formula, mapCase.data, value));
        return result.getExpr();
    }

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
    public abstract Expr translateDirect(KeyTranslator translator);

    private static <K> Expr groupSum(Map<K,? extends Expr> implement, Expr expr, Where where, Map<K, AndExpr> group) {
        Expr groupExpr = NULL;

        Where upWhere = where.not();
        for(MapCase<K> mapCase : CaseExpr.pullCases(implement)) {
            Where groupWhere = mapCase.where.and(upWhere.not());

            while(true) {
                if(groupWhere.isFalse()) break; // если false сразу вываливаемся
                Collection<InnerJoins.Entry> innerJoins = GroupExpr.inner?GroupExpr.getInnerJoins(mapCase.data,expr,groupWhere):null;
                Where innerWhere = !GroupExpr.inner || innerJoins.size()==1?groupWhere:innerJoins.iterator().next().where; // если один innerJoin то все ок, иначе нужен "полный" where

                groupExpr = groupExpr.sum(SumGroupExpr.create(BaseUtils.crossJoin(mapCase.data,group), innerWhere, expr));

                if(!GroupExpr.inner || innerJoins.size()==1) break;
                groupWhere = groupWhere.and(innerWhere.not()); // важно чтобы where не "повторился"
            }

            upWhere = upWhere.or(mapCase.where);
        }
        return groupExpr;
    }

    private static <K> Expr groupMax(Map<K,? extends Expr> implement, Expr expr, Where where, Map<K, AndExpr> group) {
        Expr groupExpr = NULL;

        Where upWhere = where.not();
        for(MapCase<K> mapCase : CaseExpr.pullCases(implement)) {
            Where groupWhere = mapCase.where.and(upWhere.not());

            Where upExprWhere = Where.FALSE;
            for(ExprCase exprCase : expr.getCases()) {  // еще один цикл по max'у погнали
                Where fullWhere = exprCase.where.and(upExprWhere.not()).and(groupWhere);

                while(true) {
                    if(fullWhere.isFalse()) break; // если false сразу вываливаемся
                    Collection<InnerJoins.Entry> innerJoins = GroupExpr.inner?GroupExpr.getInnerJoins(mapCase.data,exprCase.data,fullWhere):null;
                    Where innerWhere = !GroupExpr.inner || innerJoins.size()==1?fullWhere:innerJoins.iterator().next().where; // если один innerJoin то все ок, иначе нужен "полный" where

                    groupExpr = groupExpr.max(MaxGroupExpr.create(BaseUtils.crossJoin(mapCase.data,group), innerWhere, exprCase.data));

                    if(!GroupExpr.inner || innerJoins.size()==1) break;
                    fullWhere = fullWhere.and(innerWhere.not()); // важно чтобы where не "повторился"
                }

                upExprWhere = upExprWhere.or(exprCase.where);
            }

            upWhere = upWhere.or(mapCase.where);
        }
        return groupExpr;
    }

    private static <K> Expr groupAndBy(Map<K,? extends Expr> implement, Expr expr, Where where, boolean max, Map<K, AndExpr> group) {
        if(max)
            return groupMax(implement, expr, where, group);
        else
            return groupSum(implement, expr, where, group);
    }

    public static <K> Expr groupBy(Map<K,? extends Expr> group, Expr expr,Where where,boolean max,Map<K,? extends Expr> implement) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<K> mapCase : CaseExpr.pullCases(implement))
            result.add(mapCase.where,groupAndBy(group, expr, where, max, mapCase.data));
        return result.getExpr();
    }
}


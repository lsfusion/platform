package platform.server.data.query.exprs;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.ExprCase;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.types.Reader;
import platform.server.data.types.Type;
import platform.server.where.Where;
import platform.server.logics.DataObject;

import java.util.Collection;
import java.util.Map;

import net.jcip.annotations.Immutable;

// абстрактный класс выражений

@Immutable
abstract public class SourceExpr extends AbstractSourceJoin {

    public abstract Type getType(Where where);
    public abstract Reader getReader(Where where);

    // возвращает Where на notNull
    public Where<?> where=null;
    @ManualLazy
    public Where getWhere() {
        if(where==null)
            where = calculateWhere();
        return where;
    }
    abstract protected Where calculateWhere();

    // получает список ExprCase'ов
    public abstract ExprCaseList getCases();

    public abstract SourceExpr followFalse(Where where);

    public abstract SourceExpr getClassExpr(BaseClass baseClass);

    public abstract Where getIsClassWhere(AndClassSet set);

    public abstract Where compare(SourceExpr expr, Compare compare);

    public Where compare(DataObject data, Compare compare) {
        return compare(data.getExpr(),compare);
    }

    public Where greater(SourceExpr expr) {
        return compare(expr,Compare.GREATER).or(expr.getWhere().not());
    }

    public abstract SourceExpr scale(int coeff);

    public abstract SourceExpr sum(SourceExpr expr);

    public static SourceExpr formula(String formula, ConcreteValueClass value,Map<String,? extends SourceExpr> params) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<String> mapCase : CaseExpr.pullCases(params))
            result.add(mapCase.where,FormulaExpr.create(formula, mapCase.data, value));
        return result.getExpr();
    }

    public SourceExpr and(Where where) {
        return new ExprCaseList(where,this).getExpr();
    }
    public SourceExpr ifElse(Where where,SourceExpr elseExpr) {
        return new ExprCaseList(where,this,elseExpr).getExpr();
    }
    public SourceExpr max(SourceExpr expr) {
        return ifElse(greater(expr),expr);
    }
    public SourceExpr nvl(SourceExpr expr) {
        return ifElse(getWhere(),expr);
    }

    public abstract SourceExpr translateQuery(QueryTranslator translator);
    public abstract SourceExpr translateDirect(KeyTranslator translator);

    public SourceExpr translate(Translator translator) {
        if(translator instanceof KeyTranslator)
            return translateDirect((KeyTranslator) translator);
        else
            return translateQuery((QueryTranslator) translator);
    }

    private static <K> SourceExpr groupSum(Map<K,SourceExpr> implement, SourceExpr expr, Where where, Map<K, AndExpr> group) {
        SourceExpr groupExpr = CaseExpr.NULL;

        Where upWhere = where.not();
        for(MapCase<K> mapCase : CaseExpr.pullCases(implement)) {
            Where groupWhere = mapCase.where.and(upWhere.not());

            while(true) {
                if(groupWhere.isFalse()) break; // если false сразу вываливаемся
                Collection<InnerJoins.Entry> innerJoins = GroupExpr.inner?GroupExpr.getInnerJoins(mapCase.data,expr,groupWhere):null;
                Where innerWhere = !GroupExpr.inner || innerJoins.size()==1?groupWhere:innerJoins.iterator().next().where; // если один innerJoin то все ок, иначе нужен "полный" where

                groupExpr = groupExpr.sum(SumGroupExpr.create(innerWhere,BaseUtils.crossJoin(mapCase.data,group),expr));

                if(!GroupExpr.inner || innerJoins.size()==1) break;
                groupWhere = groupWhere.and(innerWhere.not()); // важно чтобы where не "повторился"
            }

            upWhere = upWhere.or(mapCase.where);
        }
        return groupExpr;
    }

    private static <K> SourceExpr groupMax(Map<K,SourceExpr> implement, SourceExpr expr, Where where, Map<K, AndExpr> group) {
        SourceExpr groupExpr = CaseExpr.NULL;

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

                    groupExpr = groupExpr.max(MaxGroupExpr.create(innerWhere,BaseUtils.crossJoin(mapCase.data,group),exprCase.data));

                    if(!GroupExpr.inner || innerJoins.size()==1) break;
                    fullWhere = fullWhere.and(innerWhere.not()); // важно чтобы where не "повторился"
                }

                upExprWhere = upExprWhere.or(exprCase.where);
            }

            upWhere = upWhere.or(mapCase.where);
        }
        return groupExpr;
    }

    private static <K> SourceExpr groupAndBy(Map<K,SourceExpr> implement, SourceExpr expr, Where where, boolean max, Map<K, AndExpr> group) {
        if(max)
            return groupMax(implement, expr, where, group);
        else
            return groupSum(implement, expr, where, group);
    }

    public static <K> SourceExpr groupBy(Map<K,SourceExpr> group,SourceExpr expr,Where where,boolean max,Map<K,? extends SourceExpr> implement) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<K> mapCase : CaseExpr.pullCases(implement))
            result.add(mapCase.where,groupAndBy(group, expr, where, max, mapCase.data));
        return result.getExpr();
    }
}


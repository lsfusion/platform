package platform.server.data.query.exprs;

import platform.interop.Compare;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.classes.where.ClassSet;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.MapContext;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.ExprCase;
import platform.server.data.query.exprs.cases.ExprCaseList;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.query.translators.Translator;
import platform.server.data.types.Type;
import platform.server.data.types.Reader;
import platform.server.where.Where;

import java.util.Map;

// абстрактный класс выражений

abstract public class SourceExpr extends AbstractSourceJoin<SourceExpr> {

    public abstract Type getType(Where where);
    public abstract Reader getReader(Where where);

    // возвращает Where на notNull
    private Where where=null;
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

    public abstract Where getIsClassWhere(ClassSet set);

    public abstract Where compare(SourceExpr expr,int compare);
    
    public Where greater(SourceExpr expr) {
        return compare(expr,Compare.GREATER).or(expr.getWhere().not());
    }

    public abstract SourceExpr scale(int coeff);

    public abstract SourceExpr sum(SourceExpr expr);

    public static SourceExpr formula(String formula, ConcreteValueClass value,Map<String,? extends SourceExpr> params) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<String> mapCase : CaseExpr.pullCases(params))
            result.add(new ExprCase(mapCase.where,new FormulaExpr(formula, mapCase.data, value)));
        return new CaseExpr(result);
    }
}


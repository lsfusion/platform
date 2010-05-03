package platform.server.data.expr;

import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.caches.Lazy;
import platform.server.classes.ConcreteValueClass;
import platform.server.data.query.*;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.TranslateExprLazy;
import platform.server.data.expr.where.MapWhere;
import platform.server.data.expr.cases.ExprCaseList;
import platform.server.data.expr.cases.MapCase;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.Where;

import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.Immutable;

@TranslateExprLazy
@Immutable
public class FormulaExpr extends StaticClassExpr {

    private final String formula;
    private final ConcreteValueClass valueClass;
    private final Map<String, BaseExpr> params;

    // этот конструктор напрямую можно использовать только заведомо зная что getClassWhere не null или через оболочку create 
    private FormulaExpr(String formula,Map<String, BaseExpr> params, ConcreteValueClass valueClass) {
        this.formula = formula;
        this.params = params;
        this.valueClass = valueClass;
    }

    public static Expr create(String formula, ConcreteValueClass value,Map<String,? extends Expr> params) {
        ExprCaseList result = new ExprCaseList();
        for(MapCase<String> mapCase : CaseExpr.pullCases(params))
            result.add(mapCase.where, BaseExpr.create(new FormulaExpr(formula, mapCase.data, value)));
        return result.getExpr();
    }

    public void enumerate(SourceEnumerator enumerator) {
        enumerator.fill(params);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(BaseExpr param : params.values())
            param.fillJoinWheres(joins, andWhere);
    }

    public String getSource(CompileSource compile) {
        String sourceString = formula;
        for(String prm : params.keySet())
            sourceString = sourceString.replace(prm, params.get(prm).getSource(compile));
         return "("+sourceString+")";
     }

    public Type getType(Where where) {
        return valueClass.getType();
    }

    @ParamLazy
    public Expr translateQuery(QueryTranslator translator) {
        return create(formula, valueClass, translator.translate(params));
    }

    @ParamLazy
    public BaseExpr translateDirect(KeyTranslator translator) {
        return new FormulaExpr(formula,translator.translateDirect(params),valueClass);
    }

    @Override
    public BaseExpr packFollowFalse(Where where) {
        Map<String, BaseExpr> transParams = new HashMap<String, BaseExpr>();
        for(Map.Entry<String, BaseExpr> param : params.entrySet())
            transParams.put(param.getKey(), param.getValue().packFollowFalse(where));
        assert !transParams.containsValue(null); // предпологается что сверху был andFollowFalse
        return new FormulaExpr(formula,transParams,valueClass);
    }

    // возвращает Where без следствий
    public Where calculateWhere() {
        return getWhere(params);
    }

    public DataWhereSet getFollows() {
        return InnerExpr.getExprFollows(params);
    }

    public boolean twins(AbstractSourceJoin o) {
        return formula.equals(((FormulaExpr) o).formula) && params.equals(((FormulaExpr) o).params) && valueClass.equals(((FormulaExpr) o).valueClass);
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        int hash = 0;
        for(Map.Entry<String, BaseExpr> param : params.entrySet())
            hash += param.getKey().hashCode() ^ param.getValue().hashContext(hashContext);
        return valueClass.hashCode()*31*31 + hash*31 + formula.hashCode();
    }

    public ConcreteValueClass getStaticClass() {
        return valueClass;
    }
}


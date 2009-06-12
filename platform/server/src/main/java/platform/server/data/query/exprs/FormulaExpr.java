package platform.server.data.query.exprs;

import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.query.*;
import platform.server.data.query.translators.DirectTranslator;
import platform.server.data.query.translators.Translator;
import platform.server.data.query.wheres.MapWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;


public class FormulaExpr extends StaticClassExpr {

    private final String formula;
    private final ConcreteValueClass valueClass;
    private final Map<String, AndExpr> params;

    FormulaExpr(String iFormula,Map<String,AndExpr> iParams, ConcreteValueClass iValueClass) {
        formula = iFormula;
        params = iParams;
        valueClass = iValueClass;
    }

    public int fillContext(Context context, boolean compile) {
        return context.fill(params.values(),compile);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(AndExpr param : params.values())
            param.fillJoinWheres(joins, andWhere);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String SourceString = formula;
        for(String prm : params.keySet())
            SourceString = SourceString.replace(prm, params.get(prm).getSource(queryData, syntax));
         return "("+SourceString+")";
     }

    public String toString() {
        String result = formula;
        for(String prm : params.keySet())
            result = result.replace(prm, params.get(prm).toString());
         return "("+result+")";
    }

    public Type getType(Where where) {
        return valueClass.getType();
    }

    public SourceExpr translate(Translator translator) {
        return SourceExpr.formula(formula,valueClass,translator.translate(params));
    }

    public AndExpr translateAnd(DirectTranslator translator) {
        return new FormulaExpr(formula,translator.translateAnd(params),valueClass);
    }

    @Override
    public AndExpr linearFollowFalse(Where where) {
        Map<String,AndExpr> transParams = new HashMap<String,AndExpr>();
        for(Map.Entry<String,AndExpr> param : params.entrySet())
            transParams.put(param.getKey(), param.getValue().linearFollowFalse(where));
        assert !transParams.containsValue(null); // предпологается что сверху был andFollowFalse
        return new FormulaExpr(formula,transParams,valueClass);
    }

    // возвращает Where без следствий
    protected Where calculateWhere() {
        Where result = Where.TRUE;
        for(SourceExpr param : params.values())
            result = result.and(param.getWhere());
        return result;
    }

    public DataWhereSet getFollows() {
        DataWhereSet follows = new DataWhereSet();
        for(AndExpr param : params.values())
            follows.addAll(param.getFollows());
        return follows;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof FormulaExpr && formula.equals(((FormulaExpr) o).formula) && params.equals(((FormulaExpr) o).params) && valueClass.equals(((FormulaExpr) o).valueClass);
    }

    protected int getHashCode() {
        return 31 * formula.hashCode() + params.hashCode();
    }

    // для кэша
    public boolean equals(SourceExpr expr, MapContext mapContext) {
        if(!(expr instanceof FormulaExpr)) return false;

        FormulaExpr formulaExpr = (FormulaExpr) expr;

        if(!(formula.equals(formulaExpr.formula) && params.size()==formulaExpr.params.size() && valueClass.equals(formulaExpr.valueClass))) return false;

        for(Map.Entry<String,AndExpr> param : params.entrySet())
            if(!param.getValue().equals(formulaExpr.params.get(param.getKey()), mapContext))
                return false;
        return true;
    }

    protected int getHash() {
        int hash = 0;
        for(Map.Entry<String,AndExpr> param : params.entrySet())
            hash += param.getKey().hashCode()+param.getValue().hash();
        return valueClass.hashCode()*31*31 + hash*31 + formula.hashCode();
    }

    public ConcreteValueClass getStaticClass() {
        return valueClass;
    }
}


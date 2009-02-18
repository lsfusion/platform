package platformlocal;

import java.util.*;

class FormulaExpr extends AndExpr {

    String formula;
    Type DBType;
    Map<String, SourceExpr> params;

    FormulaExpr(String iFormula,Map<String,AndExpr> iParams,Type iDBType) {
        formula = iFormula;
        params = (Map<String, SourceExpr>) (Map<String, ? extends SourceExpr>) iParams;
        DBType = iDBType;
    }

    FormulaExpr(String iFormula,Type iDBType,Map<String,SourceExpr> iParams) {
        formula = iFormula;
        params = iParams;
        DBType = iDBType;
    }

    // линейный конструктор (сумма/разница)
    FormulaExpr(SourceExpr Op1,SourceExpr Op2,boolean Sum) {
        formula = "prm1"+(Sum?"+":"-")+"prm2";
        params = new HashMap<String,SourceExpr>();
        params.put("prm1",Op1);
        params.put("prm2",Op2);
        DBType = Op1.getType();
    }

    // линейный конструктор (коэффициент)
    FormulaExpr(SourceExpr Expr,Integer Coeff) {
        formula = Coeff+"*prm1";
        params = new HashMap<String,SourceExpr>();
        params.put("prm1",Expr);
        DBType = Expr.getType();
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        for(SourceExpr Param : params.values())
            Param.fillJoins(joins, values);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(SourceExpr param : params.values())
            param.fillJoinWheres(joins, andWhere);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String SourceString = formula;
        for(String Prm : params.keySet())
            SourceString = SourceString.replace(Prm, params.get(Prm).getSource(queryData, syntax));
         return "("+SourceString+")";
     }

    public String toString() {
        String Result = formula;
        for(String Prm : params.keySet())
            Result = Result.replace(Prm, params.get(Prm).toString());
         return "("+Result+")";
    }

    Type getType() {
        return DBType;
    }

    public SourceExpr translate(Translator translator) {
        MapCaseList<String> caseList = CaseExpr.translateCase(params, translator, false, false);
        if(caseList==null)
            return this;

        ExprCaseList result = new ExprCaseList();
        for(MapCase<String> mapCase : caseList)  // здесь напрямую потому как MapCaseList уже все проверил
            result.add(new ExprCase(mapCase.where,SourceExpr.containsNull(mapCase.data)?getType().getExpr(null):new FormulaExpr(formula,mapCase.data,DBType)));
        return result.getExpr(getType());
    }

    // возвращает Where без следствий
    Where calculateWhere() {
        Where Result = Where.TRUE;
        for(SourceExpr Param : params.values())
            Result = Result.and(Param.getWhere());
        return Result;
    }

    DataWhereSet getFollows() {
        DataWhereSet Follows = new DataWhereSet();
        for(SourceExpr Param : params.values())
            Follows.addAll(((AndExpr)Param).getFollows());
        return Follows;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof FormulaExpr && formula.equals(((FormulaExpr) o).formula) && params.equals(((FormulaExpr) o).params);
    }

    public int hashCode() {
        return 31 * formula.hashCode() + params.hashCode();
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(!(expr instanceof FormulaExpr)) return false;

        FormulaExpr FormulaExpr = (FormulaExpr) expr;

        if(!formula.equals(FormulaExpr.formula) || params.size()!=FormulaExpr.params.size()) return false;

        for(Map.Entry<String,SourceExpr> Param : params.entrySet())
            if(!Param.getValue().equals(FormulaExpr.params.get(Param.getKey()), mapExprs, mapWheres))
                return false;
        return true;
    }

    int getHash() {
        int Hash = 1;
        for(Map.Entry<String,SourceExpr> Param : params.entrySet())
            Hash += Param.getKey().hashCode()+Param.getValue().hash();
        return Hash*31 + formula.hashCode();
    }
}


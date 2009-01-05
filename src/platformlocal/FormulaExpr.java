package platformlocal;

import java.util.*;

class FormulaExpr extends AndExpr {

    String Formula;
    Type DBType;
    Map<String, SourceExpr> Params;

    FormulaExpr(String iFormula,Map<String,AndExpr> iParams,Type iDBType) {
        Formula = iFormula;
        Params = (Map<String, SourceExpr>) (Map<String, ? extends SourceExpr>) iParams;
        DBType = iDBType;
    }

    FormulaExpr(String iFormula,Type iDBType,Map<String,SourceExpr> iParams) {
        Formula = iFormula;
        Params = iParams;
        DBType = iDBType;
    }

    // линейный конструктор (сумма/разница)
    FormulaExpr(SourceExpr Op1,SourceExpr Op2,boolean Sum) {
        Formula = "prm1"+(Sum?"+":"-")+"prm2";
        Params = new HashMap<String,SourceExpr>();
        Params.put("prm1",Op1);
        Params.put("prm2",Op2);
        DBType = Op1.getType();
    }

    // линейный конструктор (коэффициент)
    FormulaExpr(SourceExpr Expr,Integer Coeff) {
        Formula = Coeff+"*prm1";
        Params = new HashMap<String,SourceExpr>();
        Params.put("prm1",Expr);
        DBType = Expr.getType();
    }

    public <J extends Join> void fillJoins(List<J> Joins) {
        for(SourceExpr Param : Params.values())
            Param.fillJoins(Joins);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> Joins, IntraWhere AndWhere) {
        for(SourceExpr Param : Params.values())
            Param.fillJoinWheres(Joins,AndWhere);
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String SourceString = Formula;
        for(String Prm : Params.keySet())
            SourceString = SourceString.replace(Prm, Params.get(Prm).getSource(QueryData, Syntax));
         return "("+SourceString+")";
     }

    public String toString() {
        String Result = Formula;
        for(String Prm : Params.keySet())
            Result = Result.replace(Prm, Params.get(Prm).toString());
         return Result;
    }

    Type getType() {
        return DBType;
    }

    public SourceExpr translate(Translator Translator) {
        MapCaseList<String> CaseList = CaseExpr.translateCase(Params, Translator, false);
        if(CaseList==null)
            return this;

        ExprCaseList Result = new ExprCaseList();
        for(MapCase<String> Case : CaseList)  // здесь напрямую потому как MapCaseList уже все проверил
            Result.add(new ExprCase(Case.Where,SourceExpr.containsNull(Case.Data)?new ValueExpr(null,getType()):new FormulaExpr(Formula,Case.Data,DBType)));
        return Result.getExpr();
    }

    // возвращает IntraWhere без следствий
    IntraWhere getWhere() {
        IntraWhere Result = new InnerWhere();
        for(SourceExpr Param : Params.values())
            Result = Result.in(Param.getWhere());
        return Result;
    }

    Map<DataWhere,Boolean> CacheFollow = new IdentityHashMap<DataWhere,Boolean>();
    boolean follow(DataWhere Where) {
        if(!Main.ActivateCaches) return calculateFollow(Where);
        Boolean Result = CacheFollow.get(Where);
        if(Result==null) {
            Result = calculateFollow(Where);
            CacheFollow.put(Where,Result);
        }

        return Result;
    }
    
    boolean calculateFollow(DataWhere Where) {
        for(SourceExpr Param : Params.values())
            if(((AndExpr)Param).follow(Where)) return true;
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaExpr that = (FormulaExpr) o;

        return Formula.equals(that.Formula) && Params.equals(that.Params);
    }

    public int hashCode() {
        int result;
        result = Formula.hashCode();
        result = 31 * result + Params.hashCode();
        return result;
    }

    // для кэша
    boolean equals(SourceExpr Expr, Map<ObjectExpr, ObjectExpr> MapExprs, Map<JoinWhere, JoinWhere> MapWheres) {
        if(!(Expr instanceof FormulaExpr)) return false;

        FormulaExpr FormulaExpr = (FormulaExpr) Expr;

        if(!Formula.equals(FormulaExpr.Formula) || Params.size()!=FormulaExpr.Params.size()) return false;

        for(Map.Entry<String,SourceExpr> Param : Params.entrySet())
            if(!Param.getValue().equals(FormulaExpr.Params.get(Param.getKey())))
                return false;
        return true;
    }

    int hash() {
        int Hash = 1;
        for(Map.Entry<String,SourceExpr> Param : Params.entrySet())
            Hash += Param.getKey().hashCode()+Param.getValue().hash();
        return Hash*31 + Formula.hashCode();
    }
}

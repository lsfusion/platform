package platformlocal;

import java.util.*;

class FormulaExpr extends AndExpr {

    String Formula;
    Type DBType;
    Map<String, SourceExpr> params;

    FormulaExpr(String iFormula,Map<String,AndExpr> iParams,Type iDBType) {
        Formula = iFormula;
        params = (Map<String, SourceExpr>) (Map<String, ? extends SourceExpr>) iParams;
        DBType = iDBType;
    }

    FormulaExpr(String iFormula,Type iDBType,Map<String,SourceExpr> iParams) {
        Formula = iFormula;
        params = iParams;
        DBType = iDBType;
    }

    // линейный конструктор (сумма/разница)
    FormulaExpr(SourceExpr Op1,SourceExpr Op2,boolean Sum) {
        Formula = "prm1"+(Sum?"+":"-")+"prm2";
        params = new HashMap<String,SourceExpr>();
        params.put("prm1",Op1);
        params.put("prm2",Op2);
        DBType = Op1.getType();
    }

    // линейный конструктор (коэффициент)
    FormulaExpr(SourceExpr Expr,Integer Coeff) {
        Formula = Coeff+"*prm1";
        params = new HashMap<String,SourceExpr>();
        params.put("prm1",Expr);
        DBType = Expr.getType();
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        for(SourceExpr Param : params.values())
            Param.fillJoins(Joins, Values);
    }

    public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        for(SourceExpr param : params.values()) {
            Where siblingWhere = new AndWhere();
            for(SourceExpr siblingParam : params.values())
                if(siblingParam!=param)
                    siblingWhere = siblingWhere.and(siblingParam.getWhere());
            param.fillJoinWheres(joins, andWhere.and(siblingWhere));
        }
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String SourceString = Formula;
        for(String Prm : params.keySet())
            SourceString = SourceString.replace(Prm, params.get(Prm).getSource(QueryData, Syntax));
         return "("+SourceString+")";
     }

    public String toString() {
        String Result = Formula;
        for(String Prm : params.keySet())
            Result = Result.replace(Prm, params.get(Prm).toString());
         return "("+Result+")";
    }

    Type getType() {
        return DBType;
    }

    public SourceExpr translate(Translator Translator) {
        MapCaseList<String> CaseList = CaseExpr.translateCase(params, Translator, false);
        if(CaseList==null)
            return this;

        ExprCaseList Result = new ExprCaseList();
        for(MapCase<String> Case : CaseList)  // здесь напрямую потому как MapCaseList уже все проверил
            Result.add(new ExprCase(Case.Where,SourceExpr.containsNull(Case.Data)?new NullExpr(getType()):new FormulaExpr(Formula,Case.Data,DBType)));
        return Result.getExpr();
    }

    // возвращает Where без следствий
    Where getWhere() {
        Where Result = new AndWhere();
        for(SourceExpr Param : params.values())
            Result = Result.and(Param.getWhere());
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
        for(SourceExpr Param : params.values())
            if(((AndExpr)Param).follow(Where)) return true;
        return false;
    }
    Set<DataWhere> getFollows() {
        Set<DataWhere> Follows = new HashSet<DataWhere>();
        for(SourceExpr Param : params.values())
            Follows.addAll(((AndExpr)Param).getFollows());
        return Follows;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FormulaExpr that = (FormulaExpr) o;

        return Formula.equals(that.Formula) && params.equals(that.params);
    }

    public int hashCode() {
        int result;
        result = Formula.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(!(expr instanceof FormulaExpr)) return false;

        FormulaExpr FormulaExpr = (FormulaExpr) expr;

        if(!Formula.equals(FormulaExpr.Formula) || params.size()!=FormulaExpr.params.size()) return false;

        for(Map.Entry<String,SourceExpr> Param : params.entrySet())
            if(!Param.getValue().equals(FormulaExpr.params.get(Param.getKey()), mapExprs, mapWheres))
                return false;
        return true;
    }

    int getHash() {
        int Hash = 1;
        for(Map.Entry<String,SourceExpr> Param : params.entrySet())
            Hash += Param.getKey().hashCode()+Param.getValue().hash();
        return Hash*31 + Formula.hashCode();
    }
}


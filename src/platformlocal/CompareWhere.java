package platformlocal;

import java.util.*;

class CompareWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    SourceExpr Operator1;
    SourceExpr Operator2;

    static final int EQUALS = 0;
    static final int GREATER = 1;
    static final int LESS = 2;
    static final int GREATER_EQUALS = 3;
    static final int LESS_EQUALS = 4;
    static final int NOT_EQUALS = 5;

    int Compare;

    CompareWhere(AndExpr iOperator1,AndExpr iOperator2,int iCompare) {
        this((SourceExpr)iOperator1,(SourceExpr)iOperator2,iCompare);
    }

    // временно чтобы отличать
    CompareWhere(SourceExpr iOperator1,SourceExpr iOperator2,int iCompare) {
        Operator1 = iOperator1;
        Operator2 = iOperator2;
        Compare = iCompare;
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return Operator1.getSource(QueryData, Syntax) + getCompare(Compare) + Operator2.getSource(QueryData, Syntax);
    }

    static String getCompare(int Compare) {
        return (Compare==EQUALS?"=":(Compare==GREATER?">":(Compare==LESS?"<":(Compare==GREATER_EQUALS?">=":(Compare==LESS_EQUALS?"<=":"<>")))));
    }
    static int reverse(int Compare) {
        switch(Compare) {
            case EQUALS: return NOT_EQUALS;
            case GREATER: return LESS_EQUALS;
            case LESS: return GREATER_EQUALS;
            case GREATER_EQUALS: return LESS;
            case LESS_EQUALS: return GREATER;
            default: throw new RuntimeException("Не должно быть");
        }
    }

    static boolean compare(TypedObject Object1,TypedObject Object2,int Compare) {

        if(Object1.Value.equals(Object2.Value))
            return (Compare==EQUALS || Compare==GREATER_EQUALS || Compare==LESS_EQUALS);

        if(Compare==GREATER_EQUALS || Compare==GREATER)
            return Object1.type.greater(Object1.Value,Object2.Value);

        if(Compare==LESS_EQUALS || Compare==LESS)
            return Object1.type.greater(Object2.Value,Object1.Value);

        return false;
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы 
    public String getNotSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String Op1Source = Operator1.getSource(QueryData, Syntax);
        String Result = Op1Source + " IS NULL";
        String Op2Source = Operator2.getSource(QueryData, Syntax);
        if(!(Operator2 instanceof NullExpr))
            Result = Result + " OR " + Op2Source + " IS NULL";
        return "(" + Result + " OR " + Op1Source + getCompare(reverse(Compare)) + Op2Source + ")";
    }

    public String toString() {
        return Operator1.toString() + getCompare(Compare) + Operator2.toString(); 
    }

    public Where getCaseWhere(MapCase<Integer> Case) {
        AndExpr CaseOp1 = Case.Data.get(0);
        AndExpr CaseOp2 = Case.Data.get(1);
        if(CaseOp1.getWhere().means(CaseOp2.getWhere().not())) // проверим может значения изначально не равны
            return new OrWhere();
        else {
/*            if(CaseOp1 instanceof ValueExpr) { // нельзя так как навредит кэшу
                if(CaseOp2 instanceof ValueExpr) {
                    if(compare(((ValueExpr)CaseOp1).Object,((ValueExpr)CaseOp2).Object,Compare))
                        return new AndWhere();
                    else
                        return new OrWhere();
                } else
                    return new CompareWhere(CaseOp1,CaseOp2, reverse(Compare));
            } else*/
                return new CompareWhere(CaseOp1,CaseOp2,Compare);
        }
    }

    public Where translate(Translator Translator) {
        Map<Integer,SourceExpr> MapExprs = new HashMap<Integer, SourceExpr>();
        MapExprs.put(0,Operator1);
        MapExprs.put(1,Operator2);
        return CaseExpr.translateCase(MapExprs,Translator,true).getWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        Operator1.fillJoins(Joins, Values);
        Operator2.fillJoins(Joins, Values);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        Operator1.fillJoinWheres(Joins,AndWhere);
        Operator2.fillJoinWheres(Joins,AndWhere);
    }

    boolean calculateFollow(DataWhere Where) {
        return Where.equals(this) || ((AndExpr)Operator1).follow(Where) || ((AndExpr)Operator2).follow(Where);
    }
    Set<DataWhere> getFollows() {
        Set<DataWhere> Follows = new HashSet<DataWhere>();
        Follows.add(this);
        Follows.addAll(((AndExpr)Operator1).getFollows());
        Follows.addAll(((AndExpr)Operator2).getFollows());
        return Follows;
    }

    public JoinWheres getInnerJoins() {
        if(Operator1 instanceof KeyExpr && Operator2 instanceof ValueExpr && Compare==EQUALS)
            return new JoinWheres(this,new AndWhere());

        Where InJoinWhere = new AndWhere();
        if(Operator1 instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr)Operator1).From.InJoin);
        if(Operator2 instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr)Operator2).From.InJoin);
        return new JoinWheres(InJoinWhere,this);
    }

    public boolean equals(Object o) {
        return this==o || (o instanceof CompareWhere && Compare==((CompareWhere)o).Compare && Operator1.equals(((CompareWhere)o).Operator1) && Operator2.equals(((CompareWhere)o).Operator2));
    }

    public int hashCode() {
        int result;
        result = Operator1.hashCode();
        result = 31 * result + Operator2.hashCode();
        result = 31 * result + Compare;
        return result;
    }

    public Where copy() {
        return new CompareWhere(Operator1,Operator2,Compare);
    }

    // для кэша
    public boolean equals(Where Where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return Where instanceof CompareWhere && Compare == ((CompareWhere)Where).Compare &&
                Operator1.equals(((CompareWhere)Where).Operator1,mapExprs,mapWheres) &&
                Operator2.equals(((CompareWhere)Where).Operator2,mapExprs,mapWheres);
    }

    public int getHash() {
        return Compare + Operator1.hash()*31 + Operator2.hash()*31*31;
    }
}


class InListWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    AndExpr Expr;
    String Values;

    InListWhere(AndExpr iExpr, Collection<Integer> SetValues) {
        Expr = iExpr;
        Values = "";
        for(Integer Value : SetValues)
            Values = (Values.length()==0?"":Values+',') + Value;
    }

    InListWhere(AndExpr iExpr, String iValues) {
        Expr = iExpr;
        Values = iValues;
    }

    public String getSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        return Expr.getSource(QueryData, Syntax) + " IN (" + Values + ")";
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    public String getNotSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String ExprSource = Expr.getSource(QueryData, Syntax);
        return "(" + ExprSource + " IS NULL OR NOT " + ExprSource + " IN (" + Values + "))";
    }

    public String toString() {
        return Expr.toString() + " IN (" + Values + ")";
    }

    public Where getCaseWhere(MapCase<Integer> Case) {
        return new InListWhere(Case.Data.get(0),Values);
    }

    public Where translate(Translator Translator) {
        Map<Integer,SourceExpr> MapExprs = new HashMap<Integer, SourceExpr>();
        MapExprs.put(0,Expr);
        return CaseExpr.translateCase(MapExprs,Translator,true).getWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> Joins, Set<ValueExpr> Values) {
        Expr.fillJoins(Joins, Values);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        Expr.fillJoinWheres(Joins,AndWhere);
    }

    boolean calculateFollow(DataWhere Where) {
        return Where==this || Expr.follow(Where);
    }
    Set<DataWhere> getFollows() {
        Set<DataWhere> Follows = new HashSet<DataWhere>();
        Follows.add(this);
        Follows.addAll(Expr.getFollows());
        return Follows;
    }

    public Where copy() {
        return new InListWhere(Expr,Values);
    }

    public JoinWheres getInnerJoins() {
        Where InJoinWhere = new AndWhere();
        if(Expr instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr)Expr).From.InJoin);
        return new JoinWheres(InJoinWhere,this);
    }

    // для кэша
    public boolean equals(Where Where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return Where instanceof InListWhere && Values.equals(((InListWhere)Where).Values) &&
                Expr.equals(((InListWhere)Where).Expr,mapExprs,mapWheres);
    }

    public int getHash() {
        return Expr.hash() + Values.hashCode()*31;
    }

    int getHashCode() {
        return Expr.hashCode() + Values.hashCode()*31;
    }
}

package platformlocal;

import java.util.*;

class CompareWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    SourceExpr operator1;
    SourceExpr operator2;

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
        operator1 = iOperator1;
        operator2 = iOperator2;
        Compare = iCompare;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return operator1.getSource(queryData, syntax) + getCompare(Compare) + operator2.getSource(queryData, syntax);
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

        if(Object1.value.equals(Object2.value))
            return (Compare==EQUALS || Compare==GREATER_EQUALS || Compare==LESS_EQUALS);

        if(Compare==GREATER_EQUALS || Compare==GREATER)
            return Object1.type.greater(Object1.value,Object2.value);

        if(Compare==LESS_EQUALS || Compare==LESS)
            return Object1.type.greater(Object2.value,Object1.value);

        return false;
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы 
    public String getNotSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String Op1Source = operator1.getSource(QueryData, Syntax);
        String Result = Op1Source + " IS NULL";
        String Op2Source = operator2.getSource(QueryData, Syntax);
        if(!(operator2 instanceof NullExpr))
            Result = Result + " OR " + Op2Source + " IS NULL";
        return "(" + Result + " OR " + Op1Source + getCompare(reverse(Compare)) + Op2Source + ")";
    }

    public String toString() {
        return operator1.toString() + getCompare(Compare) + operator2.toString();
    }

    public Where getCaseWhere(MapCase<Integer> Case) {
        AndExpr CaseOp1 = Case.data.get(0);
        AndExpr CaseOp2 = Case.data.get(1);
        if(CaseOp1.getWhere().means(CaseOp2.getWhere().not())) // проверим может значения изначально не равны
            return Where.FALSE;
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
        MapExprs.put(0, operator1);
        MapExprs.put(1, operator2);
        return CaseExpr.translateCase(MapExprs,Translator,true, false).getWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        operator1.fillJoins(joins, values);
        operator2.fillJoins(joins, values);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        operator1.fillJoinWheres(Joins,AndWhere);
        operator2.fillJoinWheres(Joins,AndWhere);
    }

    DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet(((AndExpr) operator1).getFollows());
        follows.addAll(((AndExpr) operator2).getFollows());
        return follows;
    }

    public JoinWheres getInnerJoins() {
        if(operator1 instanceof KeyExpr && operator2 instanceof ValueExpr && Compare==EQUALS)
            return new JoinWheres(this,Where.TRUE);

        Where InJoinWhere = Where.TRUE;
        if(operator1 instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr) operator1).from.inJoin);
        if(operator2 instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr) operator2).from.inJoin);
        return new JoinWheres(InJoinWhere,this);
    }

    public boolean equals(Object o) {
        return this==o || (o instanceof CompareWhere && Compare==((CompareWhere)o).Compare && operator1.equals(((CompareWhere)o).operator1) && operator2.equals(((CompareWhere)o).operator2));
    }

    public int hashCode() {
        int result;
        result = operator1.hashCode();
        result = 31 * result + operator2.hashCode();
        result = 31 * result + Compare;
        return result;
    }

    public Where copy() {
        return new CompareWhere(operator1, operator2,Compare);
    }

    // для кэша
    public boolean equals(Where Where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return Where instanceof CompareWhere && Compare == ((CompareWhere)Where).Compare &&
                operator1.equals(((CompareWhere)Where).operator1,mapExprs,mapWheres) &&
                operator2.equals(((CompareWhere)Where).operator2,mapExprs,mapWheres);
    }

    public int getHash() {
        return Compare + operator1.hash()*31 + operator2.hash()*31*31;
    }
}


class InListWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    AndExpr expr;
    String Values;

    InListWhere(AndExpr iExpr, Collection<Integer> SetValues) {
        expr = iExpr;
        Values = "";
        for(Integer Value : SetValues)
            Values = (Values.length()==0?"":Values+',') + Value;
    }

    InListWhere(AndExpr iExpr, String iValues) {
        expr = iExpr;
        Values = iValues;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return expr.getSource(queryData, syntax) + " IN (" + Values + ")";
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    public String getNotSource(Map<QueryData, String> QueryData, SQLSyntax Syntax) {
        String ExprSource = expr.getSource(QueryData, Syntax);
        return "(" + ExprSource + " IS NULL OR NOT " + ExprSource + " IN (" + Values + "))";
    }

    public String toString() {
        return expr.toString() + " IN (" + Values + ")";
    }

    public Where getCaseWhere(MapCase<Integer> Case) {
        return new InListWhere(Case.data.get(0),Values);
    }

    public Where translate(Translator Translator) {
        Map<Integer,SourceExpr> MapExprs = new HashMap<Integer, SourceExpr>();
        MapExprs.put(0, expr);
        return CaseExpr.translateCase(MapExprs,Translator,true, false).getWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        expr.fillJoins(joins, values);
    }

    protected void fillDataJoinWheres(MapWhere<JoinData> Joins, Where AndWhere) {
        expr.fillJoinWheres(Joins,AndWhere);
    }

    DataWhereSet getExprFollows() {
        return expr.getFollows();
    }

    public Where copy() {
        return new InListWhere(expr,Values);
    }

    public JoinWheres getInnerJoins() {
        Where InJoinWhere = Where.TRUE;
        if(expr instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr) expr).from.inJoin);
        return new JoinWheres(InJoinWhere,this);
    }

    // для кэша
    public boolean equals(Where Where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return Where instanceof InListWhere && Values.equals(((InListWhere)Where).Values) &&
                expr.equals(((InListWhere)Where).expr,mapExprs,mapWheres);
    }

    public int getHash() {
        return expr.hash() + Values.hashCode()*31;
    }

    int getHashCode() {
        return expr.hashCode() + Values.hashCode()*31;
    }
}

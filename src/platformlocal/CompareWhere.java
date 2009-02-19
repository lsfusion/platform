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

    int compare;

    CompareWhere(AndExpr iOperator1,AndExpr iOperator2,int iCompare) {
        this((SourceExpr)iOperator1,(SourceExpr)iOperator2,iCompare);
    }

    // временно чтобы отличать
    CompareWhere(SourceExpr iOperator1,SourceExpr iOperator2,int iCompare) {
        operator1 = iOperator1;
        operator2 = iOperator2;
        compare = iCompare;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return operator1.getSource(queryData, syntax) + getCompare(operator2,compare) + operator2.getSource(queryData, syntax);
    }

    static boolean containsMask(String string) {
        return string.contains("%") || string.contains("_");  
    }
    static String getCompare(SourceExpr expr, int compare) {
        if((compare==EQUALS || compare==NOT_EQUALS) && expr instanceof ValueExpr && ((ValueExpr)expr).object.type instanceof StringType && containsMask((String)((ValueExpr)expr).object.value))
            return (compare==EQUALS?" LIKE ":" NOT LIKE ");
        else
            return (compare==EQUALS?"=":(compare==GREATER?">":(compare==LESS?"<":(compare==GREATER_EQUALS?">=":(compare==LESS_EQUALS?"<=":"<>")))));
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
    public String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String op1Source = operator1.getSource(queryData, syntax);
        String result = op1Source + " IS NULL";
        String op2Source = operator2.getSource(queryData, syntax);
        if(!(operator2 instanceof NullExpr))
            result = result + " OR " + op2Source + " IS NULL";
        return "(" + result + " OR " + op1Source + getCompare(operator2,reverse(compare)) + op2Source + ")";
    }

    public String toString() {
        return operator1.toString() + getCompare(operator2,compare) + operator2.toString();
    }

    public Where getCaseWhere(MapCase<Integer> cCase) {
        AndExpr CaseOp1 = cCase.data.get(0);
        AndExpr CaseOp2 = cCase.data.get(1);
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
                return new CompareWhere(CaseOp1,CaseOp2, compare);
        }
    }

    public Where translate(Translator translator) {
        Map<Integer,SourceExpr> mapExprs = new HashMap<Integer, SourceExpr>();
        mapExprs.put(0, operator1);
        mapExprs.put(1, operator2);
        return CaseExpr.translateCase(mapExprs,translator,true,false).getWhere(this);
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
        if(operator1 instanceof KeyExpr && operator2 instanceof ValueExpr && compare ==EQUALS)
            return new JoinWheres(this,Where.TRUE);

        Where InJoinWhere = Where.TRUE;
        if(operator1 instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr) operator1).from.inJoin);
        if(operator2 instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr) operator2).from.inJoin);
        return new JoinWheres(InJoinWhere,this);
    }

    public boolean equals(Object o) {
        return this==o || (o instanceof CompareWhere && compare ==((CompareWhere)o).compare && operator1.equals(((CompareWhere)o).operator1) && operator2.equals(((CompareWhere)o).operator2));
    }

    public int hashCode() {
        return 31 * (31 * operator1.hashCode() + operator2.hashCode()) + 1 << compare;
    }

    public Where copy() {
        return new CompareWhere(operator1, operator2, compare);
    }

    // для кэша
    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return where instanceof CompareWhere && compare == ((CompareWhere)where).compare &&
                operator1.equals(((CompareWhere)where).operator1,mapExprs,mapWheres) &&
                operator2.equals(((CompareWhere)where).operator2,mapExprs,mapWheres);
    }

    public int getHash() {
        return 1 << compare + operator1.hash()*31 + operator2.hash()*31*31;
    }
}


class InListWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    AndExpr expr;
    String values;

    InListWhere(AndExpr iExpr, Collection<Integer> SetValues) {
        expr = iExpr;
        values = "";
        for(Integer Value : SetValues)
            values = (values.length()==0?"": values +',') + Value;
    }

    InListWhere(AndExpr iExpr, String iValues) {
        expr = iExpr;
        values = iValues;
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return expr.getSource(queryData, syntax) + " IN (" + values + ")";
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    public String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String exprSource = expr.getSource(queryData, syntax);
        String notSource = "NOT " + exprSource + " IN (" + values + ")";
        if(expr.getWhere().isTrue())
            return notSource;
        else
            return "(" + exprSource + " IS NULL OR " + notSource + ")";
    }

    public String toString() {
        return expr.toString() + " IN (" + values + ")";
    }

    public Where getCaseWhere(MapCase<Integer> cCase) {
        return new InListWhere(cCase.data.get(0), values);
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
        return new InListWhere(expr, values);
    }

    public JoinWheres getInnerJoins() {
        Where InJoinWhere = Where.TRUE;
        if(expr instanceof JoinExpr)
            InJoinWhere = InJoinWhere.and(((JoinExpr) expr).from.inJoin);
        return new JoinWheres(InJoinWhere,this);
    }

    // для кэша
    public boolean equals(Where where, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        return where instanceof InListWhere && values.equals(((InListWhere)where).values) &&
                expr.equals(((InListWhere)where).expr,mapExprs,mapWheres);
    }

    public int getHash() {
        return expr.hash() + values.hashCode()*31;
    }

    public int hashCode() {
        return expr.hashCode() + values.hashCode()*31;
    }

    public boolean equals(Object obj) {
        return this==obj || (obj instanceof InListWhere && expr.equals(((InListWhere)obj).expr) && values.equals(((InListWhere)obj).values));
    }
}

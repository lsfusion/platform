package platform.server.data.query.wheres;

import platform.interop.Compare;
import platform.server.data.TypedObject;
import platform.server.data.query.*;
import platform.server.data.query.exprs.*;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.StringType;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CompareWhere extends DataWhere implements CaseWhere<MapCase<Integer>> {

    public SourceExpr operator1;
    public SourceExpr operator2;

    int compare;

    public CompareWhere(AndExpr iOperator1, AndExpr iOperator2,int iCompare) {
        this((SourceExpr)iOperator1,(SourceExpr)iOperator2,iCompare);
    }

    // временно чтобы отличать
    public CompareWhere(SourceExpr iOperator1, SourceExpr iOperator2,int iCompare) {
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
        if((compare== Compare.EQUALS || compare== Compare.NOT_EQUALS) && expr instanceof ValueExpr && ((ValueExpr)expr).object.type instanceof StringType && containsMask((String)((ValueExpr)expr).object.value))
            return (compare== Compare.EQUALS ?" LIKE ":" NOT LIKE ");
        else
            return (compare== Compare.EQUALS ?"=":(compare== Compare.GREATER ?">":(compare== Compare.LESS ?"<":(compare== Compare.GREATER_EQUALS ?">=":(compare== Compare.LESS_EQUALS ?"<=":"<>")))));
    }
    static int reverse(int compare) {
        switch(compare) {
            case Compare.EQUALS: return Compare.NOT_EQUALS;
            case Compare.GREATER: return Compare.LESS_EQUALS;
            case Compare.LESS: return Compare.GREATER_EQUALS;
            case Compare.GREATER_EQUALS: return Compare.LESS;
            case Compare.LESS_EQUALS: return Compare.GREATER;
            default: throw new RuntimeException("Не должно быть");
        }
    }

    static boolean compare(TypedObject object1, TypedObject object2,int compare) {

        if(object1.value.equals(object2.value))
            return (compare== Compare.EQUALS || compare== Compare.GREATER_EQUALS || compare== Compare.LESS_EQUALS);

        if(compare== Compare.GREATER_EQUALS || compare== Compare.GREATER)
            return object1.type.greater(object1.value,object2.value);

        if(compare== Compare.LESS_EQUALS || compare== Compare.LESS)
            return object1.type.greater(object2.value,object1.value);

        return false;
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    protected String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
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
        Map<Integer, SourceExpr> mapExprs = new HashMap<Integer, SourceExpr>();
        mapExprs.put(0, operator1);
        mapExprs.put(1, operator2);
        return CaseExpr.translateCase(mapExprs,translator,true,false).getWhere(this);
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        operator1.fillJoins(joins, values);
        operator2.fillJoins(joins, values);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    public DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet(((AndExpr) operator1).getFollows());
        follows.addAll(((AndExpr) operator2).getFollows());
        return follows;
    }

    public JoinWheres getInnerJoins() {
        if(operator1 instanceof KeyExpr && operator2 instanceof ValueExpr && compare == Compare.EQUALS)
            return new JoinWheres(this, Where.TRUE);

        Where inJoinWhere = Where.TRUE;
        if(operator1 instanceof JoinExpr)
            inJoinWhere = inJoinWhere.and(((JoinExpr) operator1).from.inJoin);
        if(operator2 instanceof JoinExpr)
            inJoinWhere = inJoinWhere.and(((JoinExpr) operator2).from.inJoin);
        return new JoinWheres(inJoinWhere,this);
    }

    public boolean equals(Object o) {
        return this==o || (o instanceof CompareWhere && compare ==((CompareWhere)o).compare && operator1.equals(((CompareWhere)o).operator1) && operator2.equals(((CompareWhere)o).operator2));
    }

    protected int getHashCode() {
        return 31 * (31 * operator1.hashCode() + operator2.hashCode()) + 1 << compare;
    }

    public Where copy() {
        return new CompareWhere(operator1, operator2, compare);
    }

    // для кэша
    public boolean equals(Where where, Map<ValueExpr, ValueExpr> mapValues, Map<KeyExpr, KeyExpr> mapKeys, MapJoinEquals mapJoins) {
        return where instanceof CompareWhere && compare == ((CompareWhere)where).compare &&
                operator1.equals(((CompareWhere)where).operator1, mapValues, mapKeys, mapJoins) &&
                operator2.equals(((CompareWhere)where).operator2, mapValues, mapKeys, mapJoins);
    }

    protected int getHash() {
        return 1 << compare + operator1.hash()*31 + operator2.hash()*31*31;
    }
}

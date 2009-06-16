package platform.server.data.query.wheres;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.classes.StringClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.*;
import platform.server.data.query.exprs.*;
import platform.server.data.query.translators.Translator;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.DataWhere;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Map;


public class CompareWhere extends DataWhere {

    public final AndExpr operator1;
    public final AndExpr operator2;

    final int compare;

    public CompareWhere(AndExpr iOperator1, AndExpr iOperator2,int iCompare) {
        operator1 = iOperator1;
        operator2 = iOperator2;
        compare = iCompare;

        assert !(operator1 instanceof ValueExpr && operator2 instanceof KeyExpr && compare==Compare.EQUALS);        
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        return operator1.getSource(queryData, syntax) + getCompare(operator2, compare) + operator2.getSource(queryData, syntax);
    }

    static boolean containsMask(String string) {
        return string.contains("%") || string.contains("_");
    }
    static String getCompare(SourceExpr expr, int compare) {
        if((compare== Compare.EQUALS || compare== Compare.NOT_EQUALS) && expr instanceof ValueExpr && ((ValueExpr)expr).objectClass instanceof StringClass && containsMask((String)((ValueExpr)expr).object))
            return (compare== Compare.EQUALS ?" LIKE ":" NOT LIKE ");
        else
            return (compare== Compare.EQUALS ?"=":(compare== Compare.GREATER ?">":(compare== Compare.LESS ?"<":(compare== Compare.GREATER_EQUALS ?">=":(compare== Compare.LESS_EQUALS ?"<=":"<>")))));
    }

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    protected String getNotSource(Map<QueryData, String> queryData, SQLSyntax syntax) {
        String op1Source = operator1.getSource(queryData, syntax);
        String result = op1Source + " IS NULL";
        String op2Source = operator2.getSource(queryData, syntax);
        result = result + " OR " + op2Source + " IS NULL";
        return "(" + result + " OR " + op1Source + getCompare(operator2, Compare.not(compare)) + op2Source + ")";
    }

    public String toString() {
        return operator1.toString() + getCompare(operator2,compare) + operator2.toString();
    }

    public Where translate(Translator translator) {
        return operator1.translate(translator).compare(operator2.translate(translator),compare);
    }

    @Override
    public Where linearFollowFalse(Where falseWhere) {
        falseWhere = falseWhere.orMeans(not());
        
        return new CompareWhere(operator1.linearFollowFalse(falseWhere),operator2.linearFollowFalse(falseWhere),compare);
    }

    public int fillContext(Context context, boolean compile) {
        int level = -1;
        level = BaseUtils.max(operator1.fillContext(context, compile),level);
        level = BaseUtils.max(operator2.fillContext(context, compile),level);
        return level;
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    public DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet(operator1.getFollows());
        follows.addAll(operator2.getFollows());
        return follows;
    }

    public InnerJoins getInnerJoins() {
        if(operator1 instanceof KeyExpr && operator2 instanceof ValueExpr && compare == Compare.EQUALS)
            return new InnerJoins(this,this);
        assert !(operator2 instanceof KeyExpr && operator1 instanceof ValueExpr && compare == Compare.EQUALS);
        return operator1.getWhere().and(operator2.getWhere()).getInnerJoins().and(new InnerJoins(Where.TRUE,this));
    }

/*    public Where getJoinWhere() {
        if(operator1 instanceof KeyExpr && operator2 instanceof ValueExpr && compare == Compare.EQUALS)
            return this;

        Where innerWhere = Where.FALSE;
        for(InnerJoins.Entry inner : operator1.getWhere().and(operator2.getWhere()).getInnerJoins())
            innerWhere = innerWhere.or(inner.mean);

        Where result = Where.TRUE;
        for(Where dataWhere : innerWhere.getOr())
            if(dataWhere instanceof DataWhere)
                result = result.and(dataWhere);

        return result;

//        Where inJoinWhere = Where.TRUE;
//        if(operator1 instanceof JoinExpr)
//            inJoinWhere = inJoinWhere.and(((JoinExpr) operator1).from.getWhere());
//        if(operator2 instanceof JoinExpr)
//            inJoinWhere = inJoinWhere.and(((JoinExpr) operator2).from.getWhere());
//        return inJoinWhere;
    }*/

    public boolean equals(Object o) {
        return this==o || (o instanceof CompareWhere && compare ==((CompareWhere)o).compare && operator1.equals(((CompareWhere)o).operator1) && operator2.equals(((CompareWhere)o).operator2));
    }

    protected int getHashCode() {
        return 31 * (31 * operator1.hashCode() + operator2.hashCode()) + 1 << compare;
    }

    // для кэша
    public boolean equals(Where where, MapContext mapContext) {
        return where instanceof CompareWhere && compare == ((CompareWhere)where).compare &&
                operator1.equals(((CompareWhere)where).operator1, mapContext) &&
                operator2.equals(((CompareWhere)where).operator2, mapContext);
    }

    protected int getHash() {
        return 1 << compare + operator1.hash()*31 + operator2.hash()*31*31;
    }

    public ClassExprWhere calculateClassWhere() {

        ClassExprWhere classWhere1 = operator1.getWhere().getClassWhere();
        ClassExprWhere classWhere2 = operator2.getWhere().getClassWhere();

        if(compare==Compare.EQUALS) { // если идет равенство
            if(operator2 instanceof VariableClassExpr) {
                if(operator1 instanceof StaticClassExpr)
                    classWhere2 = classWhere2.and(new ClassExprWhere((VariableClassExpr)operator2,((StaticClassExpr)operator1).getStaticClass()));
                else {
                    if(operator1 instanceof JoinExpr)
                        classWhere1 = classWhere1.andEquals((JoinExpr)operator1,(VariableClassExpr)operator2);
                    if(operator2 instanceof JoinExpr)
                        classWhere2 = classWhere2.andEquals((JoinExpr)operator2,(VariableClassExpr)operator1);
                }
            } else
            if(operator1 instanceof VariableClassExpr)
                classWhere1 = classWhere1.and(new ClassExprWhere((VariableClassExpr)operator1,((StaticClassExpr)operator2).getStaticClass()));
        }
        
        return classWhere1.and(classWhere2);
    }
}

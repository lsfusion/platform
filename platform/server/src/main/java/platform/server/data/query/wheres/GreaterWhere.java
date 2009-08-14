package platform.server.data.query.wheres;

import platform.server.data.query.exprs.*;
import platform.server.data.query.CompileSource;
import platform.server.data.query.InnerJoins;
import platform.server.data.query.HashContext;
import platform.server.data.query.translators.Translator;
import platform.server.data.classes.StringClass;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.where.Where;
import platform.server.where.DataWhereSet;
import platform.server.caches.ParamLazy;
import platform.interop.Compare;

// если operator1 не null и больше operator2 или operator2 null
public class GreaterWhere extends CompareWhere {

    private GreaterWhere(AndExpr iOperator1, AndExpr iOperator2) {
        super(iOperator1, iOperator2);
    }

    public static Where create(AndExpr operator1, AndExpr operator2) {
        return create(new GreaterWhere(operator1, operator2));
    }

    public boolean equals(Object o) {
        return this==o || (o instanceof GreaterWhere && operator1.equals(((GreaterWhere)o).operator1) && operator2.equals(((GreaterWhere)o).operator2));
    }

    public int hashContext(HashContext hashContext) {
        return 1 + operator1.hashContext(hashContext)*31 + operator2.hashContext(hashContext)*31*31;
    }

    @ParamLazy
    public Where translate(Translator translator) {
        return operator1.translate(translator).compare(operator2.translate(translator),Compare.GREATER);
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        return new GreaterWhere(operator1.packFollowFalse(falseWhere),operator2.packFollowFalse(falseWhere));
    }

    /*
    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return operator1.getSource(compile) + ">" + operator2.getSource(compile);

        String op2Source = operator2.getSource(compile);
        String result = operator1.getSource(compile) + ">" + op2Source;
        if(!operator2.getWhere().isTrue())
            result = "(" + op2Source + " IS NULL OR " + result + ")";
        return result;
    }

    @Override
    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String op2Source = operator2.getSource(compile);
        String result = "NOT " + op1Source + ">" + op2Source;
//        if(!operator2.getWhere().isTrue())
//            result = "(" + op2Source + " IS NOT NULL AND " + result + ")";
        if(!operator1.getWhere().isTrue())
            result = "(" + op1Source + " IS NULL OR " + result + ")";
        return result;
    }

    public DataWhereSet getExprFollows() {
        return operator1.getFollows();
    }

    public InnerJoins getInnerJoins() {
        return operator1.getWhere().getInnerJoins().and(new InnerJoins(this));
    }

    public ClassExprWhere calculateClassWhere() {
        return operator1.getWhere().getClassWhere();
    }
      */

    // а вот тут надо извратится и сделать Or проверив сначала null'ы
    public String getSource(CompileSource compile) {
        return operator1.getSource(compile) + ">" + operator2.getSource(compile);
    }

    @Override
    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String result = operator1.getWhere().isTrue()?"":op1Source + " IS NULL";
        String op2Source = operator2.getSource(compile);
        if(!operator2.getWhere().isTrue())
            result = (result.length()==0?"":result+" OR ") + op2Source + " IS NULL";
        String compare = "NOT " + op1Source + ">" + op2Source;
        if(result.length()==0)
            return compare;
        else
            return "(" + result + " OR " + compare + ")";
    }

    public DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet(operator1.getFollows());
        follows.addAll(operator2.getFollows());
        return follows;
    }

    public InnerJoins getInnerJoins() {
        return operator1.getWhere().and(operator2.getWhere()).getInnerJoins().and(new InnerJoins(this));
    }

    public ClassExprWhere calculateClassWhere() {
        return operator1.getWhere().getClassWhere().and(operator2.getWhere().getClassWhere());
    }
}

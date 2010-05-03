package platform.server.data.expr.where;

import platform.interop.Compare;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.query.AbstractSourceJoin;
import platform.server.data.query.CompileSource;
import platform.server.caches.hash.HashContext;
import platform.server.caches.Lazy;
import platform.server.data.query.InnerJoins;
import platform.server.data.expr.BaseExpr;
import platform.server.data.where.Where;
import platform.base.BaseUtils;
import net.jcip.annotations.Immutable;

// если operator1 не null и больше operator2 или operator2 null
@Immutable
public class GreaterWhere extends CompareWhere {

    private GreaterWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(BaseUtils.hashEquals(operator1,operator2))
            return Where.FALSE;
        return create(new GreaterWhere(operator1, operator2));
    }

    public boolean twins(AbstractSourceJoin o) {
        return operator1.equals(((GreaterWhere)o).operator1) && operator2.equals(((GreaterWhere)o).operator2);
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        return 1 + operator1.hashContext(hashContext)*31 + operator2.hashContext(hashContext)*31*31;
    }

    protected CompareWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new GreaterWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.GREATER;
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

    public InnerJoins getInnerJoins() {
        return operator1.getWhere().and(operator2.getWhere()).getInnerJoins().and(new InnerJoins(this));
    }
    public ClassExprWhere calculateClassWhere() {
        return operator1.getWhere().getClassWhere().and(operator2.getWhere().getClassWhere());
    }
}

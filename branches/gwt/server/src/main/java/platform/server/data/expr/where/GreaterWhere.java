package platform.server.data.expr.where;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.where.Where;

// если operator1 не null и больше operator2 или operator2 null
public class GreaterWhere extends CompareWhere {

    // public только для symmetricWhere
    public GreaterWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    public static Where create(BaseExpr operator1, BaseExpr operator2) {
        if(BaseUtils.hashEquals(operator1,operator2))
            return Where.FALSE;
        return create(new GreaterWhere(operator1, operator2));
    }

    @IdentityLazy
    public int hashOuter(HashContext hashContext) {
        return 1 + operator1.hashOuter(hashContext)*31 + operator2.hashOuter(hashContext)*31*31;
    }

    protected CompareWhere createThis(BaseExpr operator1, BaseExpr operator2) {
        return new GreaterWhere(operator1, operator2);
    }

    protected Compare getCompare() {
        return Compare.GREATER;
    }

    protected String getCompareSource(CompileSource compile) {
        return ">";
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
}

package platform.server.data.expr.where.extra;

import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.query.CompileSource;
import platform.server.data.where.OrObjectWhere;
import platform.server.data.where.OrWhere;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;

import java.util.Map;

public abstract class CompareWhere<This extends CompareWhere<This>> extends BinaryWhere<This> {
    
    protected CompareWhere(BaseExpr operator1, BaseExpr operator2) {
        super(operator1, operator2);
    }

    // такой же where но без прямых сравнений
    private Where symmetricWhere = null;
    @ManualLazy
    // A>B = !(A=B) AND !(B>A) AND A AND B
    // A=B = !(A>B) AND !(B>A) AND A AND B
    private Where getSymmetricWhere() {
        if(symmetricWhere==null) {
            GreaterWhere backCompare = new GreaterWhere(operator2, operator1, false);
            CompareWhere signCompare;
            if (this instanceof GreaterWhere) {
                assert !((GreaterWhere)this).orEquals;
                signCompare = new EqualsWhere(operator1, operator2);
            } else
                signCompare = new GreaterWhere(operator1, operator2, false);

            OrObjectWhere[] operators = getOperandWhere().getOr();
            OrObjectWhere[] symmetricOrs = new OrObjectWhere[operators.length+2];
            System.arraycopy(operators, 0, symmetricOrs, 0, operators.length);
            symmetricOrs[operators.length] = backCompare.not();
            symmetricOrs[operators.length+1] = signCompare.not(); 

            symmetricWhere = toWhere(symmetricOrs);
        }
        return symmetricWhere;
    }

    public boolean checkTrue(Where where) {
        return OrWhere.checkTrue(getSymmetricWhere(),where);
    }

    public static <K> Where compare(Map<K,? extends Expr> map1,Map<K,? extends Expr> map2) {
        Where where = TRUE;
        for(Map.Entry<K,? extends Expr> entry : map1.entrySet())
            where = where.and(entry.getValue().compare(map2.get(entry.getKey()), Compare.EQUALS));
        return where;
    }

    public static <K> Where compareValues(Map<K,? extends Expr> map,Map<K, DataObject> mapValues) {
        Where where = TRUE;
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            where = where.and(entry.getValue().compare(mapValues.get(entry.getKey()), Compare.EQUALS));
        return where;
    }

    protected String getNotSource(CompileSource compile) {
        String op1Source = operator1.getSource(compile);
        String op2Source = operator2.getSource(compile);

        String result = "";
        if(!compile.means(operator1.getNotNullWhere()))
            result = op1Source + " IS NULL";
        if(!compile.means(operator2.getNotNullWhere()))
            result = (result.length()==0?"":result+" OR ") + op2Source + " IS NULL";
        String compare = "NOT " + op1Source + getCompareSource(compile) + op2Source;
        if(result.length()==0)
            return compare;
        else
            return "(" + result + " OR " + compare + ")";
    }
}

package platform.server.data.expr.where.extra;

import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.CompileSource;
import platform.server.data.where.OrObjectWhere;
import platform.server.data.where.OrWhere;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;

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

    public static <K> Where compare(ImMap<K, ? extends Expr> map1, ImMap<K, ? extends Expr> map2) {
        Where where = TRUE;
        for(int i=0,size=map1.size();i<size;i++)
            where = where.and(map1.getValue(i).compare(map2.get(map1.getKey(i)), Compare.EQUALS));
        return where;
    }

    public static <K> Where compareExprValues(ImRevMap<K, KeyExpr> mapKeys, ImMap<K, ? extends Expr> mapValues) {
        return compare(mapKeys.filterIncl(mapValues.keys()), mapValues);
    }

    public static <K> Where compareValues(ImMap<K,? extends Expr> map,ImMap<K, DataObject> mapValues) {
        Where where = TRUE;
        for(int i=0,size=map.size();i<size;i++)
            where = where.and(map.getValue(i).compare(mapValues.get(map.getKey(i)), Compare.EQUALS));
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

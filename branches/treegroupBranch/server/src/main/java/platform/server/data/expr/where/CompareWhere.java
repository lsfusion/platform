package platform.server.data.expr.where;

import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.caches.ParamLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.*;
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
            GreaterWhere backCompare = new GreaterWhere(operator2, operator1);
            CompareWhere signCompare = this instanceof GreaterWhere ? new EqualsWhere(operator1, operator2) : new GreaterWhere(operator1, operator2);

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
        Where where = Where.TRUE;
        for(Map.Entry<K,? extends Expr> entry : map1.entrySet())
            where = where.and(entry.getValue().compare(map2.get(entry.getKey()), Compare.EQUALS));
        return where;
    }

    public static <K> Where compareValues(Map<K,? extends Expr> map,Map<K, DataObject> mapValues) {
        Where where = Where.TRUE;
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            where = where.and(entry.getValue().compare(mapValues.get(entry.getKey()), Compare.EQUALS));
        return where;
    }
}

package platform.server.data.expr.where;

import platform.server.data.query.JoinData;
import platform.server.data.query.SourceEnumerator;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.DataWhere;
import platform.server.data.where.OrWhere;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.interop.Compare;

import java.util.Map;


public abstract class CompareWhere extends DataWhere {

    public final BaseExpr operator1;
    public final BaseExpr operator2;

    protected CompareWhere(BaseExpr iOperator1, BaseExpr iOperator2) {
        operator1 = iOperator1;
        operator2 = iOperator2;
    }

    public void enumerate(SourceEnumerator enumerator) {
        operator1.enumerate(enumerator);
        operator2.enumerate(enumerator);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    public boolean checkTrue(Where where) {
        // A>B = !(A=B) AND !(B>A) AND A AND B
        // A=B = !(A>B) AND !(B>A) AND A AND B
        return  OrWhere.orTrue(operator1.getWhere(),where) &&
                OrWhere.orTrue(operator2.getWhere(),where) &&
                GreaterWhere.create(operator2, operator1).means(where) &&
                (this instanceof GreaterWhere?EqualsWhere.create(operator1, operator2):GreaterWhere.create(operator1, operator2)).means(where);
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

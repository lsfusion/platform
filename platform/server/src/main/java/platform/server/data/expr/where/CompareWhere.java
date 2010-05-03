package platform.server.data.expr.where;

import platform.server.data.query.JoinData;
import platform.server.data.query.SourceEnumerator;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.DataWhere;
import platform.server.data.where.OrWhere;
import platform.server.data.where.Where;
import platform.server.data.where.DataWhereSet;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.logics.DataObject;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashKeyExprsContext;
import platform.server.caches.hash.HashContext;
import platform.interop.Compare;

import java.util.Map;

import net.jcip.annotations.Immutable;


@Immutable
public abstract class CompareWhere<This extends CompareWhere<This>> extends DataWhere {

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

    public DataWhereSet getExprFollows() {
        DataWhereSet follows = new DataWhereSet(operator1.getFollows());
        follows.addAll(operator2.getFollows());
        return follows;
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

    protected abstract This createThis(BaseExpr operator1, BaseExpr operator2);
    protected abstract Compare getCompare();

    @ParamLazy
    public Where translateDirect(KeyTranslator translator) {
        return createThis(operator1.translateDirect(translator),operator2.translateDirect(translator));
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return operator1.translateQuery(translator).compare(operator2.translateQuery(translator),getCompare());
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        // нет гарантии как в случае 0>L(A+B) OR !(A OR B) что не упакуется до пустого
        BaseExpr packedOperator1 = operator1.andFollowFalse(falseWhere);
        if(packedOperator1==null) return FALSE;
        BaseExpr packedOperator2 = operator2.andFollowFalse(falseWhere);
        if(packedOperator2==null) return FALSE;

        Map<KeyExpr, BaseExpr> keyExprs = falseWhere.not().getKeyExprs();
        if(keyExprs.size()>0) {
            HashContext hashKeyExprs = new HashKeyExprsContext(keyExprs);
            if(operator1.hashContext(hashKeyExprs)==operator2.hashContext(hashKeyExprs)) {
                QueryTranslator translator = new QueryTranslator(keyExprs,false);
                if(operator1.translateQuery(translator).equals(operator2.translateQuery(translator)))
                    return ((CompareWhere)this) instanceof EqualsWhere?Where.TRUE:Where.FALSE;
            }
        }
  
        return createThis(packedOperator1,packedOperator2);
    }


}

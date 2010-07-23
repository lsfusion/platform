package platform.server.data.expr.where;

import net.jcip.annotations.Immutable;
import platform.interop.Compare;
import platform.server.caches.ManualLazy;
import platform.server.caches.ParamLazy;
import platform.server.caches.hash.HashContext;
import platform.server.caches.hash.HashKeyExprsContext;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.VariableExprSet;
import platform.server.data.query.JoinData;
import platform.server.data.query.ContextEnumerator;
import platform.server.data.query.innerjoins.ObjectJoinSets;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.where.*;
import platform.server.logics.DataObject;

import java.util.Map;


@Immutable
public abstract class CompareWhere<This extends CompareWhere<This>> extends DataWhere {
    
    public final BaseExpr operator1;
    public final BaseExpr operator2;

    protected CompareWhere(BaseExpr operator1, BaseExpr operator2) {
        this.operator1 = operator1;
        this.operator2 = operator2;
    }

    public void enumerate(ContextEnumerator enumerator) {
        operator1.enumerate(enumerator);
        operator2.enumerate(enumerator);
    }

    public void fillDataJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        operator1.fillJoinWheres(joins,andWhere);
        operator2.fillJoinWheres(joins,andWhere);
    }

    public DataWhereSet calculateFollows() {
        return new DataWhereSet(new VariableExprSet(operator1, operator2));
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

            OrObjectWhere[] operators = operator1.getWhere().and(operator2.getWhere()).getOr();
            OrObjectWhere[] symmetricOrs = new OrObjectWhere[operators.length+2];
            System.arraycopy(operators, 0, symmetricOrs, 0, operators.length);
            symmetricOrs[operators.length] = backCompare.not();
            symmetricOrs[operators.length+1] = signCompare.not(); 

            symmetricWhere = toWhere(symmetricOrs, FollowDeep.PACK);
        }
        return symmetricWhere;
    }

    public boolean checkTrue(Where where) {
        return OrWhere.orTrue(getSymmetricWhere(),where);
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
    public Where translate(MapTranslate translator) {
        return createThis(operator1.translate(translator),operator2.translate(translator));
    }
    @ParamLazy
    public Where translateQuery(QueryTranslator translator) {
        return operator1.translateQuery(translator).compare(operator2.translateQuery(translator),getCompare());
    }

    @Override
    public Where packFollowFalse(Where falseWhere) {
        // нет гарантии как в случае 0>L(A+B) OR !(A OR B) что не упакуется до пустого
        return operator1.followFalse(falseWhere,true).compare(operator2.followFalse(falseWhere,true),getCompare());
//        return operator1.packFollowFalse(falseWhere).compare(operator2.packFollowFalse(falseWhere),getCompare());
    }

    public ObjectJoinSets groupObjectJoinSets() {
        return operator1.getWhere().and(operator2.getWhere()).groupObjectJoinSets().and(new ObjectJoinSets(this));
    }
}

package platform.server.data.query.innerjoins;

import platform.base.TwinImmutableInterface;
import platform.base.TwinImmutableObject;
import platform.server.caches.IdentityLazy;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.extra.EqualsWhere;
import platform.server.data.query.ExprEqualsJoin;
import platform.server.data.query.stat.WhereJoin;
import platform.server.data.query.stat.WhereJoins;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class KeyEqual extends TwinImmutableObject implements DNFWheres.Interface<KeyEqual> {

    public final Map<KeyExpr, BaseExpr> keyExprs;

    public KeyEqual() {
        this.keyExprs = new HashMap<KeyExpr, BaseExpr>();
    }

    public KeyEqual(KeyExpr key, BaseExpr expr) {
        keyExprs = Collections.singletonMap(key, expr);
    }

    public KeyEqual(Map<KeyExpr, BaseExpr> keyExprs) {
        this.keyExprs = keyExprs;
    }

    public KeyEqual and(KeyEqual and) {
        Map<KeyExpr,BaseExpr> result = new HashMap<KeyExpr,BaseExpr>(keyExprs);
        for(Map.Entry<KeyExpr,BaseExpr> andKeyExpr : and.keyExprs.entrySet()) {
            BaseExpr expr = result.get(andKeyExpr.getKey());
            if(expr==null || !expr.isValue())
                result.put(andKeyExpr.getKey(),andKeyExpr.getValue());
        }
        return new KeyEqual(result);
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isEmpty() {
        return keyExprs.isEmpty();
    }

    @IdentityLazy
    public QueryTranslator getTranslator() {
        return new PartialQueryTranslator(keyExprs);
    }

    public Where getWhere() {
        Where equalsWhere = Where.TRUE;
        for(Map.Entry<KeyExpr,BaseExpr> keyExpr : keyExprs.entrySet())
            equalsWhere = equalsWhere.and(EqualsWhere.create(keyExpr.getKey(),keyExpr.getValue()));
        return equalsWhere;
    }

    public boolean twins(TwinImmutableInterface o) {
        return keyExprs.equals(((KeyEqual) o).keyExprs);
    }

    public int immutableHashCode() {
        return keyExprs.hashCode();
    }

    public static KeyEqual getKeyEqual(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new KeyEqual((KeyExpr) operator1, operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new KeyEqual((KeyExpr) operator2, operator1);
        return new KeyEqual();
    }

    public WhereJoins getWhereJoins() {
        WhereJoin[] wheres = new WhereJoin[keyExprs.size()]; int iw = 0;
        for(Map.Entry<KeyExpr, BaseExpr> keyExpr : keyExprs.entrySet())
            wheres[iw++] = new ExprEqualsJoin(keyExpr.getKey(), keyExpr.getValue());
        return new WhereJoins(wheres);
    }
}

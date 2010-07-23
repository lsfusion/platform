package platform.server.data.query.innerjoins;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.where.DNFWheres;
import platform.server.data.where.Where;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.translator.PartialQueryTranslator;
import platform.base.BaseUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class KeyEqual implements DNFWheres.Interface<KeyEqual> {

    private final Map<KeyExpr, BaseExpr> keyExprs;

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
        return new KeyEqual(BaseUtils.merge(keyExprs, and.keyExprs));
    }

    public boolean isFalse() {
        return false;
    }

    public boolean isEmpty() {
        return keyExprs.isEmpty();
    }

    public QueryTranslator getTranslator() {
        return new PartialQueryTranslator(keyExprs);
    }

    public Where getWhere() {
        Where equalsWhere = Where.TRUE;
        for(Map.Entry<KeyExpr,BaseExpr> keyExpr : keyExprs.entrySet())
            equalsWhere = equalsWhere.and(EqualsWhere.create(keyExpr.getKey(),keyExpr.getValue()));
        return equalsWhere;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof KeyEqual && keyExprs.equals(((KeyEqual) o).keyExprs);
    }

    @Override
    public int hashCode() {
        return keyExprs.hashCode();
    }

    public static boolean isKeyEqual(BaseExpr operator1, BaseExpr operator2) {
        return operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1);
    }

    public static KeyEqual getKeyEqual(BaseExpr operator1, BaseExpr operator2) {
        if(operator1 instanceof KeyExpr && !operator2.hasKey((KeyExpr) operator1))
            return new KeyEqual((KeyExpr) operator1, operator2);
        if(operator2 instanceof KeyExpr && !operator1.hasKey((KeyExpr) operator2))
            return new KeyEqual((KeyExpr) operator2, operator1);
        return new KeyEqual();
    }

}
